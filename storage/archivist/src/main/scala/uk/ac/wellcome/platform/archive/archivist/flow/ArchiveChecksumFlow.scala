package uk.ac.wellcome.platform.archive.archivist.flow

import java.security.MessageDigest

import akka.stream._
import akka.stream.stage._
import akka.util.ByteString

/** This is a custom graph stage that receives a single stream of bytes in,
  * and emits two streams: the bytes for uploading to another service, and
  * the checksum of the final string.
  *
  *                                      +------> O  DigestCalculator.out
  *                                     /            (bytes to upload)
  *     DigestCalculator.in   O -------+
  *     (original bytes)                \
  *                                      +------> O  DigestCalculatorFinished.out
  *                                                  (checksum)
  *
  */
class ArchiveChecksumFlow(algorithm: String)
    extends GraphStage[FanOutShape2[ByteString, ByteString, String]] {

  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[ByteString]("DigestCalculator.out")
  val outDigest = Outlet[String]("DigestCalculatorFinished.out")

  override val shape = new FanOutShape2[ByteString, ByteString, String](
    in = in,
    out0 = out,
    out1 = outDigest
  )

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      // The digest tracks the hash of the uploaded bytes -- it holds state
      // outside the bytes flowing through the graph.
      private val digest = MessageDigest.getInstance(algorithm)

      // This OutHandler configures the copy of the bytes emitted to
      // DigestCalculator.out -- the bytes that get uploaded elsewhere.
      //
      // It pulls the bytes from the inlet, and when it's done, sends the
      // checksum to the digest outlet.
      //
      //      in --- (onPull) ---> out
      //                            |
      //                   (onDownstreamFinish)
      //                            |
      //                            v
      //                        outDigest
      //
      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = pull(in)

          // This ensures the flow emits on the checksum port even if
          // whatever listens to the other out port cancels the stream.
          // In practise this could happen if the s3 upload fails. In that case,
          // we want this flow to still emit something so that higher level flows
          // can also emit a message reporting the failure.
          override def onDownstreamFinish(): Unit = {
            push(outDigest, toStringChecksum(ByteString(digest.digest())))

            digest.reset()
            completeStage()
          }
        }
      )

      // This sets up an empty pull for the onDigest outlet.  Because it
      // receives values from onDownstreamFinish/onUpstreamFinish, this
      // outlet doesn't need to do anything itself.
      //
      setHandler(outDigest, new OutHandler {
        override def onPull(): Unit = ()
      })

      // This Inhandler configures the inlet for the flow.  It pulls bytes
      // from the external source, adds them to the digest, then sends
      // them to the `out` outlet.
      //
      // When it's done, it sends the digest to the digest outlet.
      //
      //      external source --- (grab) ---> in --- (onUpstreamFinish) ---> outDigest
      //                                      ^
      //                                      |
      //                               (digest.update)
      //                                      |
      //                                      v
      //                                   digest
      //
      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val chunk = grab(in)

            digest.update(chunk.toArray)

            push(out, chunk)
          }

          // This flow can only emit a checksum when it has seen the whole stream of bytes.
          // Before that there's no way of knowing if there are any more bytes coming.
          // Therefore the calculated checksum can only ever be emitted when
          // the upstream is marked as finished.
          //
          // This ensures we always emit *something* from both outlets -- and
          // further down the pipeline, we'll notice the checksum is wrong and
          // fail the entire message.
          //
          override def onUpstreamFinish(): Unit = {
            push(outDigest, toStringChecksum(ByteString(digest.digest())))

            digest.reset()
            completeStage()
          }
        }
      )

      private def toStringChecksum(byteChecksum: ByteString) = {
        byteChecksum
          .map(0xFF & _)
          .map("%02x".format(_))
          .foldLeft("") {
            _ + _
          }
          .mkString
      }
    }
}

object ArchiveChecksumFlow {
  def apply(algorithm: String) = new ArchiveChecksumFlow(algorithm)
}
