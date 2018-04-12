package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.AllWorksIncludes
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContextExecutor

class IdentifiedWorkToVisibleDisplayWorkFlowTest
    extends FunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with ExtendedPatience {

  it("creates DisplayWorks from IdentifiedWorks") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = IdentifiedWorkToVisibleDisplayWork()

        val works = (1 to 3).map { version =>
          IdentifiedWork(
            canonicalId = "rbfhv6b4",
            title = Some("Rumblings from a rambunctious rodent"),
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.miroImageNumber,
              ontologyType = "work",
              value = "R0060400"
            ),
            version = version
          )
        }

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

  it("suppresses IdentifiedWorks with visible = false") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = IdentifiedWorkToVisibleDisplayWork()

        val visibleWorks = (1 to 3).map { version =>
          IdentifiedWork(
            canonicalId = "rbfhv6b4",
            title = Some("Rumblings from a rambunctious rodent"),
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.miroImageNumber,
              ontologyType = "work",
              value = "R0060400"
            ),
            version = version
          )
        }
        val notVisibleWork = IdentifiedWork(
          canonicalId = "rbfhv6b4",
          title = Some("Rumblings from a rambunctious rodent"),
          sourceIdentifier = SourceIdentifier(
            identifierScheme = IdentifierSchemes.miroImageNumber,
            ontologyType = "work",
            value = "R0060400"
          ),
          visible = false,
          version = 1
        )

        val eventualDisplayWorks = Source(visibleWorks :+ notVisibleWork)
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
