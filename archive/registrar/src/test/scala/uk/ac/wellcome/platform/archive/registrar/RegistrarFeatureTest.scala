package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ArchiveProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.platform.archive.registrar.fixtures.{
  Registrar => RegistrarFixture
}
import uk.ac.wellcome.platform.archive.registrar.models.{
  BagRegistrationCompleteNotification,
  StorageManifest,
  StorageManifestFactory
}
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ExtendedPatience
    with ArchiveProgressMonitorFixture
    with RegistrarFixture {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

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
        val requestId = UUID.randomUUID()
        val callbackUrl = new URI("http://localhost/archive/complete")
        withBagNotification(
          requestId,
          Some(callbackUrl),
          queuePair,
          storageBucket) { bagLocation =>
          givenArchiveProgressRecord(
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

                assertProgressRecordedRecentEvents(
                  requestId.toString,
                  Seq("registered"),
                  progressTable)
                assertProgressStatus(
                  requestId.toString,
                  ArchiveProgress.Completed,
                  progressTable)

              }
          }
        }
    }
  }
}
