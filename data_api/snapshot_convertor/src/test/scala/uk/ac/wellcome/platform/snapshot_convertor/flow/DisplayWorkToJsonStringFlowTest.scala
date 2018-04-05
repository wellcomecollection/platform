package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.JsonTestUtil

import scala.concurrent.ExecutionContextExecutor

class DisplayWorkToJsonStringFlowTest
    extends FunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with JsonTestUtil {

  it("converts a DisplayWork to a correctly-formatted JSON string") {
    withActorSystem { actorSystem =>
      implicit val executionContext: ExecutionContextExecutor =
        actorSystem.dispatcher
      withMaterializer(actorSystem) { materializer =>
        val flow = DisplayWorkToJsonStringFlow()

        val displayWork = DisplayWork(
          id = "cguztabd",
          title = "A collection of curled-up cats in Croatia"
        )

        val expectedJsonString = s"""{
          "id": "${displayWork.id}",
          "title": "${displayWork.title}",
          "creators": [ ],
          "subjects": [ ],
          "type": "Work"
        }"""

        val futureJsonString = Source.single(displayWork)
          .via(flow)
          .runWith(Sink.head)(materializer)

        whenReady(futureJsonString) { jsonString =>
          assertJsonStringsAreEqual(jsonString, expectedJsonString)}
      }
    }
  }
}
