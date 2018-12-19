package uk.ac.wellcome.platform.snapshot_generator.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.{V1WorksIncludes, V2WorksIncludes}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.test.fixtures.Akka

class IdentifiedWorkToVisibleDisplayWorkFlowTest
    extends FunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with IntegrationPatience
    with WorksGenerators {

  it("creates V1 DisplayWorks from IdentifiedWorks") {
    withMaterializer { implicit materializer =>
      val flow = IdentifiedWorkToVisibleDisplayWork(
        toDisplayWork = DisplayWorkV1.apply(_, V1WorksIncludes.includeAll()))

      val works = createIdentifiedWorks(count = 3).toList

      val eventualDisplayWorks = Source(works)
        .via(flow)
        .runWith(Sink.seq)

      whenReady(eventualDisplayWorks) { displayWorks =>
        val expectedDisplayWorks = works.map {
          DisplayWorkV1(_, includes = V1WorksIncludes.includeAll())
        }
        displayWorks shouldBe expectedDisplayWorks
      }
    }
  }

  it("creates V2 DisplayWorks from IdentifiedWorks") {
    withMaterializer { implicit materializer =>
      val flow = IdentifiedWorkToVisibleDisplayWork(
        toDisplayWork = DisplayWorkV2.apply(_, V2WorksIncludes.includeAll()))

      val works = createIdentifiedWorks(count = 3).toList

      val eventualDisplayWorks = Source(works)
        .via(flow)
        .runWith(Sink.seq)

      whenReady(eventualDisplayWorks) { displayWorks =>
        val expectedDisplayWorks = works.map {
          DisplayWorkV2(_, includes = V2WorksIncludes.includeAll())
        }
        displayWorks shouldBe expectedDisplayWorks
      }
    }
  }
}
