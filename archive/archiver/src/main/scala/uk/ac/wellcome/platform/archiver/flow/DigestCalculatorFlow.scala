package uk.ac.wellcome.platform.archiver.flow

import java.security.MessageDigest

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import grizzled.slf4j.Logging

class DigestCalculatorFlow(algorithm: String, checksum: String) extends GraphStage[FlowShape[ByteString, ByteString]] with Logging {
  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[ByteString]("DigestCalculator.out")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val digest = MessageDigest.getInstance(algorithm)

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val chunk = grab(in)

        digest.update(chunk.toArray)

        push(out, chunk)
      }

      override def onUpstreamFinish(): Unit = {
        val streamDigest = ByteString(digest.digest())
          .map(0xFF & _)
          .map {
            "%02x".format(_)
          }.foldLeft("") {
            _ + _
          }.mkString

        if(streamDigest != checksum) {
          fail(out, new RuntimeException(s"Checksum not matched!"))
        }

        digest.reset()

        completeStage()
      }
    })
  }
}

object DigestCalculatorFlow {
  def apply(algorithm: String, checksum: String) =
    new DigestCalculatorFlow(algorithm, checksum)
}
