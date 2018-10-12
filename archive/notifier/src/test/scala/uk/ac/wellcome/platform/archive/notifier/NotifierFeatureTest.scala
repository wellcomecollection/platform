package uk.ac.wellcome.platform.archive.notifier

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock.{
  equalToJson,
  postRequestedFor,
  urlPathEqualTo,
  _
}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.models.{
  CallbackNotification,
  DisplayIngest
}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.TimeTestFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
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
    with TimeTestFixture {

  import Progress._

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def createProgressWith(id: UUID, callbackUri: Option[URI]): Progress =
    Progress(id, uploadUri, callbackUri, Completed)

  describe("Making callbacks") {
    it("makes a POST request when it receives a Progress with a callback") {
      withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
        withNotifier {
          case (queuePair, _, notifier) =>
            val requestId = UUID.randomUUID()

            val callbackUri =
              new URI(s"http://$callbackHost:$callbackPort/callback/$requestId")

            val progress = createProgressWith(
              requestId,
              Some(callbackUri)
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
                  .withRequestBody(
                    equalToJson(toJson(DisplayIngest(progress)).get)))
            }
        }
      }
    }
  }

  describe("Updating status") {
    it("sends a ProgressUpdate when it receives Progress with a callback") {
      withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
        withNotifier {
          case (queuePair, topic, notifier) =>
            val requestId = UUID.randomUUID()

            val callbackPath = s"/callback/$requestId"
            val callbackUri = new URI(
              s"http://$callbackHost:$callbackPort" + callbackPath
            )

            stubFor(
              post(urlEqualTo(callbackPath))
                .willReturn(aResponse().withStatus(200))
            )

            val progress = createProgressWith(
              requestId,
              Some(callbackUri)
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
                  .withRequestBody(
                    equalToJson(toJson(DisplayIngest(progress)).get)))

              inside(notificationMessage[ProgressUpdate](topic)) {
                case ProgressUpdate(id, List(progressEvent), status) =>
                  id shouldBe progress.id
                  progressEvent.description shouldBe "Callback fulfilled."
                  status shouldBe Progress.CompletedCallbackSucceeded
                  assertRecent(progressEvent.createdDate)
              }
            }
        }
      }
    }

    it(
      "sends a ProgressUpdate when it receives Progress with a callback it cannot fulfill") {
      withNotifier {
        case (queuePair, topic, notifier) =>
          val requestId = UUID.randomUUID()

          val callbackUri = new URI(
            s"http://$callbackHost:$callbackPort/callback/$requestId"
          )

          val progress = createProgressWith(
            id = requestId,
            callbackUri = Some(callbackUri)
          )

          sendNotificationToSQS[CallbackNotification](
            queuePair.queue,
            CallbackNotification(requestId, callbackUri, progress)
          )

          notifier.run()

          eventually {
            inside(notificationMessage[ProgressUpdate](topic)) {
              case ProgressUpdate(id, List(progressEvent), status) =>
                id shouldBe progress.id
                progressEvent.description shouldBe s"Callback failed for: ${progress.id}, got 404 Not Found!"
                status shouldBe Progress.CompletedCallbackFailed
                assertRecent(progressEvent.createdDate)
            }
          }
      }
    }
  }
}
