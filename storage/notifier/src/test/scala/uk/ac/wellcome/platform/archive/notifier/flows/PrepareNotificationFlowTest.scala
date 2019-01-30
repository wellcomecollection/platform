package uk.ac.wellcome.platform.archive.notifier.flows

import java.util.UUID

import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll, _}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  TimeTestFixture
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Callback,
  ProgressCallbackStatusUpdate
}
import uk.ac.wellcome.platform.archive.notifier.fixtures.NotifierGenerators
import uk.ac.wellcome.test.fixtures.Akka

class PrepareNotificationFlowTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures
    with Inside
    with RandomThings
    with ProgressGenerators
    with TimeTestFixture
    with NotifierGenerators {

  describe("Processing callback responses") {
    val successfulStatuscodes =
      Table(
        "success status code",
        200,
        201,
        202,
        203,
        204,
        205,
        206,
        207,
        208,
        226
      )
    it("returns a successful ProgressUpdate when a callback succeeds") {
      withMaterializer { implicit materializer =>
        forAll(successfulStatuscodes) { responseStatus: Int =>
          val callbackResult = createCallbackResultWith(
            response = HttpResponse(responseStatus)
          )

          val eventualResult =
            Source
              .single(callbackResult)
              .via(PrepareNotificationFlow())
              .runWith(Sink.seq)

          whenReady(eventualResult) { result =>
            inside(result.head) {
              case ProgressCallbackStatusUpdate(
                  actualId,
                  callbackStatus,
                  List(progressEvent)) =>
                actualId shouldBe callbackResult.id
                progressEvent.description shouldBe "Callback fulfilled."
                callbackStatus shouldBe Callback.Succeeded
                assertRecent(progressEvent.createdDate)
            }
          }
        }
      }
    }

    val failedStatusUUID =
      UUID.fromString("12f251b8-c4a9-4afa-85de-c34ec3ed71fe")

    val failedStatusCodes =
      Table(
        ("failed status code", "msg"),
        (
          500,
          s"Callback failed for: ${failedStatusUUID.toString}, got 500 Internal Server Error!"),
        (
          400,
          "Callback failed for: ${failedStatusUUID.toString}, got 400 Bad Request!")
      )
    it(
      "returns a failed ProgressUpdate when a callback returns with a failed status code") {
      withMaterializer { implicit materializer =>
        forAll(failedStatusCodes) {
          (responseStatus: Int, expectedMsg: String) =>
            val callbackResult = createCallbackResultWith(
              id = failedStatusUUID,
              response = HttpResponse(responseStatus)
            )

            val eventualResult =
              Source
                .single(callbackResult)
                .via(PrepareNotificationFlow())
                .runWith(Sink.seq)

            whenReady(eventualResult) { result =>
              inside(result.head) {
                case ProgressCallbackStatusUpdate(
                    actualId,
                    callbackStatus,
                    List(progressEvent)) =>
                  actualId shouldBe id
                  progressEvent.description shouldBe expectedMsg
                  callbackStatus shouldBe Callback.Failed
                  assertRecent(progressEvent.createdDate)
              }
            }
        }
      }
    }

    it("returns a failed ProgressUpdate when a callback fails") {
      withMaterializer { implicit materializer =>
        val id = UUID.fromString("12f251b8-c4a9-4afa-85de-c34ec3ed71fe")
        val callbackResult = createFailedCallbackResultWith(
          id = id,
          exception = new RuntimeException("Callback exception"))

        val eventualResult =
          Source
            .single(callbackResult)
            .via(PrepareNotificationFlow())
            .runWith(Sink.seq)

        whenReady(eventualResult) { result =>
          inside(result.head) {
            case ProgressCallbackStatusUpdate(
                actualId,
                callbackStatus,
                List(progressEvent)) =>
              actualId shouldBe id
              progressEvent.description shouldBe s"Callback failed for: ${id.toString} (Callback exception)"
              callbackStatus shouldBe Callback.Failed
              assertRecent(progressEvent.createdDate)
          }
        }
      }
    }
  }
}
