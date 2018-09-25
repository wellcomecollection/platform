package uk.ac.wellcome.platform.archive.archivist.flow

import java.security.MessageDigest

import akka.stream._
import akka.stream.stage._
import akka.util.ByteString
import grizzled.slf4j.Logging

class ArchiveChecksumFlow(algorithm: String)
  extends GraphStage[FanOutShape2[ByteString, ByteString, String]]
  with Logging {

  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[ByteString]("DigestCalculator.out")
  val outDigest = Outlet[String]("DigestCalculatorFinished.out")

  override val shape = new FanOutShape2[ByteString, ByteString, String](in, out, outDigest)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private val digest = MessageDigest.getInstance(algorithm)

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)

        override def onDownstreamFinish(): Unit = {
          push(outDigest, toStringChecksum(ByteString(digest.digest())))

          digest.reset()
          completeStage()
        }
      })

      setHandler(outDigest, new OutHandler {
        override def onPull(): Unit = ()
      })

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val chunk = grab(in)

            digest.update(chunk.toArray)

            push(out, chunk)
          }

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
