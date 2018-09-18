package uk.ac.wellcome.platform.archive.archivist.streams.fanOut

import java.security.MessageDigest

import akka.stream._
import akka.stream.stage._
import akka.util.ByteString
import grizzled.slf4j.Logging

class ArchiveChecksumFanOut(algorithm: String)
  extends GraphStage[UniformFanOutShape[ByteString, ByteString]]
  with Logging {

  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[ByteString]("DigestCalculator.out")
  val outDigest = Outlet[ByteString]("DigestCalculatorFinished.out")

  override val shape = UniformFanOutShape[ByteString, ByteString](in, out, outDigest)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private val digest = MessageDigest.getInstance(algorithm)

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
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
            push(outDigest, ByteString(digest.digest()))

            digest.reset()
            completeStage()
          }
        }
      )
    }
}

object ArchiveChecksumFanOut {
  def apply(algorithm: String) = new ArchiveChecksumFanOut(algorithm)
}
