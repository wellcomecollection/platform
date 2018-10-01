package uk.ac.wellcome.platform.archive.call_backerei

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
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.flows.CallbackPayload
import uk.ac.wellcome.platform.archive.call_backerei.fixtures.{
  LocalWireMockFixture,
  CallBäckerei => CallBäckereiFixture
}
import uk.ac.wellcome.platform.archive.call_backerei.models.{
  BagRegistrationCompleteNotification,
  StorageManifest,
  StorageManifestFactory
}

import scala.concurrent.ExecutionContext.Implicits.global

class CallBäckereiFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with ProgressMonitorFixture
    with LocalWireMockFixture
    with CallBäckereiFixture {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  it("registers an archived BagIt bag from S3") {
    withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
      withCallBäckerei {
        case (
            storageBucket,
            queuePair,
            topic,
            registrar,
            hybridBucket,
            hybridTable,
            progressTable) =>
          val requestId = UUID.randomUUID()
          val callbackUrl =
            new URI(s"http://$callbackHost:$callbackPort/callback/$requestId")
          withBagNotification(
            requestId,
            Some(callbackUrl),
            queuePair,
            storageBucket) { bagLocation =>
            givenProgressRecord(
              requestId.toString,
              "upLoadUrl",
              Some(callbackUrl.toString),
              progressTable)

            registrar.run()

            eventually {
              wireMock.verifyThat(
                1,
                postRequestedFor(urlPathEqualTo(callbackUrl.getPath))
                  .withRequestBody(equalToJson(
                    toJson(CallbackPayload(requestId.toString)).get)))
            }
          }
      }
    }
  }
}
