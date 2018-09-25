package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.util.Try

class ProcessLogDiscardFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ExtendedPatience
    with ScalaFutures {

  it("process, logs & discards failed events") {
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
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
        val pldFlow = ProcessLogDiscardFlow("ProcessLogDiscardFlowTest")(func)

        val eventualResult = source
          .via(pldFlow)
          .async
          .runWith(Sink.seq)(materializer)

        whenReady(eventualResult) { result =>
          result shouldBe succeedList.map(func).map(_.get)
        }
      }
    }
  }
}
