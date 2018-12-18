package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.fixtures.Akka

class DiscardLeftFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures {

  it("discards Left values") {
    withMaterializer { implicit materializer =>
      val leftList = List("fail", "flumps").map(Left(_))

      val sourceList = List("boomer", "bust", "banana")
      val rightList = sourceList.map(Right(_))

      val list: List[Either[String, String]] = leftList ++ rightList

      val source = Source(list)
      val discardLeftFlow = DiscardLeftFlow[String, String]()

      val eventualResult = source
        .via(discardLeftFlow)
        .async
        .runWith(Sink.seq)

      whenReady(eventualResult) { result =>
        result.toList shouldBe sourceList
      }
    }
  }
}
