package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlPathEqualTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.registrar.fixtures.{LocalWireMockFixture, Registrar => RegistrarFixture}
import uk.ac.wellcome.platform.archive.registrar.flows.CallbackPayload
import uk.ac.wellcome.platform.archive.registrar.models.{BagRegistrationCompleteNotification, StorageManifest, StorageManifestFactory}
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrarFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ExtendedPatience
    with ProgressMonitorFixture
    with LocalWireMockFixture
    with RegistrarFixture {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  it("registers an archived BagIt bag from S3") {
    withLocalWireMockClient(callbackHost, callbackPort) { wireMock =>
      withRegistrar {
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

            implicit val _ = s3Client

            whenReady(StorageManifestFactory.create(bagLocation)) {
              storageManifest =>
                debug(s"Created StorageManifest: $storageManifest")

                eventually {
                  assertSnsReceivesOnly(
                    BagRegistrationCompleteNotification(
                      requestId,
                      storageManifest),
                    topic
                  )

                  assertStored[StorageManifest](
                    hybridBucket,
                    hybridTable,
                    storageManifest.id.value,
                    storageManifest
                  )

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
}
