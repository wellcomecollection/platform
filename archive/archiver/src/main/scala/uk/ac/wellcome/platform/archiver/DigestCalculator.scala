package uk.ac.wellcome.platform.archiver

import java.security.MessageDigest

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

import scala.util.{Failure, Success, Try}

class DigestCalculator(algorithm: String, checksum: String) extends GraphStage[FlowShape[ByteString, Try[String]]] {
  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[Try[String]]("DigestCalculator.out")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val digest = MessageDigest.getInstance(algorithm)

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val chunk: ByteString = grab(in)
        digest.update(chunk.toArray)
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        val streamDigest = ByteString(digest.digest())
          .map(0xFF & _)
          .map {
            "%02x".format(_)
          }.foldLeft("") {
            _ + _
          }.mkString


        val checksumVerification = verifyChecksum(streamDigest, checksum)

        emit(out, checksumVerification)
        completeStage()
      }
    })
  }

  private def verifyChecksum(expected: String, actual: String) =
    if (expected != actual) {
      Failure(new RuntimeException(
        s"Checksum validation failed! Expected $expected, got $actual."
      ))
    } else {
      Success((actual))
    }
}
