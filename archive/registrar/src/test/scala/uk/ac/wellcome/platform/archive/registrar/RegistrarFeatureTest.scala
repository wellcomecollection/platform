package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.registrar.fixtures.{
  Registrar => RegistrarFixture
}
import uk.ac.wellcome.platform.archive.registrar.models.{
  RegistrationComplete,
  BagManifest,
  BagManifestFactory
}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with ProgressMonitorFixture
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
          hybridTable,
          progressTable) =>
        val (callbackUrl, requestId) = createCallbackUrl

        withBagNotification(
          requestId,
          Some(callbackUrl),
          queuePair,
          storageBucket) { bagLocation =>
          givenProgressRecord(
            requestId.toString,
            uploadUri,
            Some(callbackUri),
            progressTable
          )

          registrar.run()

          implicit val _ = s3Client

          whenReady(BagManifestFactory.create(bagLocation)) {
            storageManifest =>
              debug(s"Created StorageManifest: $storageManifest")

              eventually {
                assertSnsReceivesOnly(
                  RegistrationComplete(
                    requestId,
                    storageManifest),
                  topic
                )

                assertStored[BagManifest](
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
