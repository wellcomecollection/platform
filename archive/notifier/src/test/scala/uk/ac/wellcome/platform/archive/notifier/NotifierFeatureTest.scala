package uk.ac.wellcome.platform.archive.notifier

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock.{
  equalToJson,
  postRequestedFor,
  urlPathEqualTo,
  _
}
import org.apache.http.HttpStatus
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  TimeTestFixture
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Callback,
  ProgressCallbackStatusUpdate,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.display._
import uk.ac.wellcome.platform.archive.notifier.fixtures.{
  LocalWireMockFixture,
  NotifierFixture
}

class NotifierFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with LocalWireMockFixture
    with NotifierFixture
    with Inside
    with RandomThings
    with ProgressGenerators
    with TimeTestFixture {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  describe("Making callbacks") {
    it("makes a POST request when it receives a Progress with a callback") {
      withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
        withNotifier {
          case (queuePair, _, notifier) =>
            val requestId = randomUUID

            val callbackUri =
              new URI(s"http://$callbackHost:$callbackPort/callback/$requestId")

            val progress = createProgressWith(
              id = requestId,
              callback = Some(createCallbackWith(uri = callbackUri))
            )

            sendNotificationToSQS(
              queuePair.queue,
              CallbackNotification(requestId, callbackUri, progress)
            )

            notifier.run()

            eventually {
              wireMock.verifyThat(
                1,
                postRequestedFor(urlPathEqualTo(callbackUri.getPath))
                  .withRequestBody(equalToJson(toJson(ResponseDisplayIngest(
                    "http://localhost/context.json",
                    progress.id,
                    DisplayLocation(
                      StandardDisplayProvider,
                      progress.sourceLocation.location.namespace,
                      progress.sourceLocation.location.key),
                    progress.callback.map(DisplayCallback(_)),
                    CreateDisplayIngestType,
                    DisplayStorageSpace(progress.space.underlying),
                    DisplayStatus(progress.status.toString),
                    progress.bag.map(bagId =>
                      IngestDisplayBag(
                        s"${bagId.space}/${bagId.externalIdentifier}")),
                    progress.events.map(event =>
                      DisplayProgressEvent(
                        event.description,
                        event.createdDate.toString)),
                    progress.createdDate.toString,
                    progress.lastModifiedDate.toString
                  )).get))
              )
            }
        }
      }
    }
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._

  val successfulStatuscodes =
    Table(
      "status code",
      HttpStatus.SC_OK,
      HttpStatus.SC_CREATED,
      HttpStatus.SC_ACCEPTED,
      HttpStatus.SC_NO_CONTENT
    )
  describe("Updating status") {
    it("sends a ProgressUpdate when it receives a successful callback") {
      forAll(successfulStatuscodes) { statusResponse: Int =>
        withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
          withNotifier {
            case (queuePair, topic, notifier) =>
              val requestId = randomUUID

              val callbackPath = s"/callback/$requestId"
              val callbackUri = new URI(
                s"http://$callbackHost:$callbackPort" + callbackPath
              )

              stubFor(
                post(urlEqualTo(callbackPath))
                  .willReturn(aResponse().withStatus(statusResponse))
              )

              val progress = createProgressWith(
                id = requestId,
                callback = Some(createCallbackWith(uri = callbackUri))
              )

              sendNotificationToSQS[CallbackNotification](
                queuePair.queue,
                CallbackNotification(requestId, callbackUri, progress)
              )

              notifier.run()

              eventually {
                wireMock.verifyThat(
                  1,
                  postRequestedFor(urlPathEqualTo(callbackUri.getPath))
                    .withRequestBody(equalToJson(toJson(ResponseDisplayIngest(
                      "http://localhost/context.json",
                      progress.id,
                      DisplayLocation(
                        StandardDisplayProvider,
                        progress.sourceLocation.location.namespace,
                        progress.sourceLocation.location.key),
                      progress.callback.map(DisplayCallback(_)),
                      CreateDisplayIngestType,
                      DisplayStorageSpace(progress.space.underlying),
                      DisplayStatus(progress.status.toString),
                      progress.bag.map(bagId =>
                        IngestDisplayBag(
                          s"${bagId.space}/${bagId.externalIdentifier}")),
                      progress.events.map(event =>
                        DisplayProgressEvent(
                          event.description,
                          event.createdDate.toString)),
                      progress.createdDate.toString,
                      progress.lastModifiedDate.toString
                    )).get))
                )

                inside(notificationMessage[ProgressUpdate](topic)) {
                  case ProgressCallbackStatusUpdate(
                      id,
                      callbackStatus,
                      List(progressEvent)) =>
                    id shouldBe progress.id
                    progressEvent.description shouldBe "Callback fulfilled."
                    callbackStatus shouldBe Callback.Succeeded
                    assertRecent(progressEvent.createdDate)
                }
              }
          }
        }
      }
    }

    it(
      "sends a ProgressUpdate when it receives Progress with a callback it cannot fulfill") {
      withNotifier {
        case (queuePair, topic, notifier) =>
          val requestId = randomUUID

          val callbackUri = new URI(
            s"http://$callbackHost:$callbackPort/callback/$requestId"
          )

          val progress = createProgressWith(
            id = requestId,
            callback = Some(createCallbackWith(uri = callbackUri))
          )

          sendNotificationToSQS[CallbackNotification](
            queuePair.queue,
            CallbackNotification(requestId, callbackUri, progress)
          )

          notifier.run()

          eventually {
            inside(notificationMessage[ProgressUpdate](topic)) {
              case ProgressCallbackStatusUpdate(
                  id,
                  callbackStatus,
                  List(progressEvent)) =>
                id shouldBe progress.id
                progressEvent.description shouldBe s"Callback failed for: ${progress.id}, got 404 Not Found!"
                callbackStatus shouldBe Callback.Failed
                assertRecent(progressEvent.createdDate)
            }
          }
      }
    }
  }
}
