package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent
import uk.ac.wellcome.test.fixtures.Akka

import scala.util.Try

class EitherFlowTest extends FunSpec with Akka with Matchers with ScalaFutures {

  it("turns a Try into an Either, wrapping a FailedEvent[In]") {
    withMaterializer { implicit materializer =>
      val e = new RuntimeException("EitherFlowTest")
      val func: String => Try[Int] = (in: String) =>
        Try {
          if (in.startsWith("f")) {
            throw e
          }

          in.length
      }

      val failList = List("fail", "flumps")
      val succeedList = List("boomer", "bust", "banana")

      val listIn = succeedList.patch(2, failList, 0)
      val source = Source(listIn)
      val eitherFlow = EitherFlow(func)

      val eventualResult = source
        .via(eitherFlow)
        .async
        .runWith(Sink.seq)

      whenReady(eventualResult) {
        result: Seq[Either[FailedEvent[String], Int]] =>
          val lefts = result.filter(_.isLeft)
          lefts.collect { case Left(l) => l } shouldBe failList.map(
            FailedEvent(e, _))

          val rights = result.filter(_.isRight)
          rights.collect { case Right(r) => r } shouldBe succeedList.map(
            func(_).get)
      }
    }
  }
}
