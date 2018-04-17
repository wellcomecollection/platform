package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.{AllWorksIncludes, WorksUtil}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContextExecutor

class IdentifiedWorkToVisibleDisplayWorkFlowTest
    extends FunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with WorksUtil {

  it("creates V1 DisplayWorks from IdentifiedWorks") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = IdentifiedWorkToVisibleDisplayWork(
          toDisplayWork = DisplayWorkV1.apply)

        val works = createWorks(count = 3).toList

        val eventualDisplayWorks = Source(works)
          .via(flow)
          .runWith(Sink.seq)(materializer)

        whenReady(eventualDisplayWorks) { displayWorks =>
          val expectedDisplayWorks = works.map {
            DisplayWorkV1(_, includes = AllWorksIncludes())
          }
          displayWorks shouldBe expectedDisplayWorks
        }
      }
    }
  }

  it("creates V2 DisplayWorks from IdentifiedWorks") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = IdentifiedWorkToVisibleDisplayWork(
          toDisplayWork = DisplayWorkV2.apply)

        val works = createWorks(count = 3).toList

        val eventualDisplayWorks = Source(works)
          .via(flow)
          .runWith(Sink.seq)(materializer)

        whenReady(eventualDisplayWorks) { displayWorks =>
          val expectedDisplayWorks = works.map {
            DisplayWorkV2(_, includes = AllWorksIncludes())
          }
          displayWorks shouldBe expectedDisplayWorks
        }
      }
    }
  }

  it("suppresses IdentifiedWorks with visible = false") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = IdentifiedWorkToVisibleDisplayWork(
          toDisplayWork = DisplayWorkV1.apply)

        val visibleWorks = createWorks(count = 3).toList
        val notVisibleWorks = createWorks(count = 2, visible = false).toList

        val eventualDisplayWorks = Source(visibleWorks ++ notVisibleWorks)
          .via(flow)
          .runWith(Sink.seq)(materializer)

        whenReady(eventualDisplayWorks) { displayWorks =>
          val expectedDisplayWorks = visibleWorks.map {
            DisplayWorkV1(_, includes = AllWorksIncludes())
          }
          displayWorks shouldBe expectedDisplayWorks
        }
      }
    }
  }
}
