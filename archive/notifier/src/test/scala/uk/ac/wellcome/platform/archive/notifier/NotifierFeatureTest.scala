package uk.ac.wellcome.platform.archive.notifier

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock.{
  equalToJson,
  postRequestedFor,
  urlPathEqualTo
}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.notifier.fixtures.{
  LocalWireMockFixture,
  NotifierFixture => notifierFixture
}
import uk.ac.wellcome.platform.archive.notifier.flows.CallbackPayload
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.Completed

class NotifierFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with LocalWireMockFixture
    with notifierFixture {

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
            queue = queuePair.queue,
            message = progress
          )

          notifier.run()

          eventually {
            wireMock.verifyThat(
              1,
              postRequestedFor(urlPathEqualTo(new URI(callbackUrl).getPath))
                .withRequestBody(
                  equalToJson(toJson(CallbackPayload(requestId.toString)).get)))
          }
      }
    }
  }

  it("doesn't make any requests if it receives a Progress without a callback") {
    withNotifier {
      case (queuePair, _, notifier) =>
        val requestId = UUID.randomUUID()

        val progress = createProgressWith(
          id = requestId,
          callbackUrl = None
        )

        sendNotificationToSQS(
          queue = queuePair.queue,
          message = progress
        )

        notifier.run()

        Thread.sleep(1000)

        eventually {
          assertQueueEmpty(queuePair.queue)
          assertQueueEmpty(queuePair.dlq)
        }
    }
  }
}
