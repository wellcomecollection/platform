package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent
import uk.ac.wellcome.test.fixtures.Akka

class LogLeftFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures {

  it("does not change events in the flow") {
    withMaterializer { implicit materializer =>
      val e = new RuntimeException("EitherFlowTest")

      val leftList = List("fail", "flumps").map(s => Left(FailedEvent(e, s)))
      val rightList = List("boomer", "bust", "banana").map(Right(_))

      val list
        : List[Either[FailedEvent[String], String]] = leftList ++ rightList

      val source = Source(list)
      val logLeftFlow = LogLeftFlow[String, String]("LogLeftFlowTest")

      val eventualResult = source
        .via(logLeftFlow)
        .async
        .runWith(Sink.seq)

      whenReady(eventualResult) { result =>
        result.toList shouldBe list
      }
    }
  }
}
