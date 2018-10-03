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
import uk.ac.wellcome.platform.archive.common.progress.fixtures.TimeTestFixture
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.Completed
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.notifier.fixtures.{
  LocalWireMockFixture,
  NotifierFixture => notifierFixture
}

class NotifierFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with LocalWireMockFixture
    with notifierFixture
    with Inside
    with TimeTestFixture {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  def createProgressWith(id: UUID, callbackUrl: Option[String]): Progress =
    Progress(
      id = id.toString,
      uploadUrl = randomAlphanumeric(25),
      callbackUrl = callbackUrl,
      result = Completed
    )

  describe("Making callbacks") {
    it("makes a POST request when it receives a Progress with a callback") {
      withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
        withNotifier {
          case (queuePair, _, notifier) =>
            val requestId = UUID.randomUUID()

            val callbackUrl =
              s"http://$callbackHost:$callbackPort/callback/$requestId"

            val progress = createProgressWith(
              id = requestId,
              callbackUrl = Some(callbackUrl)
            )

            sendNotificationToSQS(
              queuePair.queue,
              CallbackNotification(requestId.toString, callbackUrl, progress)
            )

            notifier.run()

            eventually {
              wireMock.verifyThat(
                1,
                postRequestedFor(urlPathEqualTo(new URI(callbackUrl).getPath))
                  .withRequestBody(equalToJson(toJson(progress).get)))
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
            val callbackUrl =
              s"http://$callbackHost:$callbackPort" + callbackPath

            stubFor(
              post(urlEqualTo(callbackPath))
                .willReturn(aResponse().withStatus(200))
            )

            val progress = createProgressWith(
              id = requestId,
              callbackUrl = Some(callbackUrl)
            )

            sendNotificationToSQS[CallbackNotification](
              queuePair.queue,
              CallbackNotification(requestId.toString, callbackUrl, progress)
            )

            notifier.run()

            eventually {
              wireMock.verifyThat(
                1,
                postRequestedFor(urlPathEqualTo(new URI(callbackUrl).getPath))
                  .withRequestBody(equalToJson(toJson(progress).get)))

              inside(notificationMessage[ProgressUpdate](topic)) {
                case ProgressUpdate(id, progressEvent, status) =>
                  id shouldBe progress.id
                  progressEvent.description shouldBe "Callback fulfilled."
                  status shouldBe Progress.CompletedCallbackSucceeded
                  assertRecent(progressEvent.time)
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

          val callbackUrl =
            s"http://$callbackHost:$callbackPort/callback/$requestId"

          val progress = createProgressWith(
            id = requestId,
            callbackUrl = Some(callbackUrl)
          )

          sendNotificationToSQS[CallbackNotification](
            queuePair.queue,
            CallbackNotification(requestId.toString, callbackUrl, progress)
          )

          notifier.run()

          eventually {
            inside(notificationMessage[ProgressUpdate](topic)) {
              case ProgressUpdate(id, progressEvent, status) =>
                id shouldBe progress.id
                progressEvent.description shouldBe s"Callback failed for: ${progress.id}, got 404 Not Found!"
                status shouldBe Progress.CompletedCallbackFailed
                assertRecent(progressEvent.time)
            }
          }
      }
    }
  }
}
