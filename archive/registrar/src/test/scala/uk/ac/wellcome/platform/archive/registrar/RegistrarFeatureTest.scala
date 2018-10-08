package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.registrar.fixtures.{
  Registrar => RegistrarFixture
}
import uk.ac.wellcome.platform.archive.registrar.models.{
  BagRegistrationCompleteNotification,
  StorageManifest,
  StorageManifestFactory
}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with RegistrarFixture {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val callbackHost = "localhost"
  private val callbackPort = 8080

  def createCallbackUrl = {
    val requestId = UUID.randomUUID()
    (
      new URI(
        s"http://$callbackHost:$callbackPort/callback/$requestId"
      ),
      requestId)
  }

  it("registers an archived BagIt bag from S3") {
    withRegistrar {
      case (
          storageBucket,
          queuePair,
          topic,
          registrar,
          hybridBucket,
          hybridTable) =>
        val (callbackUrl, requestId) = createCallbackUrl

        withBagNotification(
          requestId,
          Some(callbackUrl),
          queuePair,
          storageBucket) { bagLocation =>
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
              }
          }
        }
    }
  }
}
