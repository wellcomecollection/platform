package uk.ac.wellcome.platform.archive.registrar

import java.net.URI
import java.util.UUID

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.registrar.fixtures.{Registrar => RegistrarFixture}
import uk.ac.wellcome.platform.archive.registrar.models._

class RegistrarFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with IntegrationPatience
    with RegistrarFixture with Inside {
  implicit val _ = s3Client

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

            eventually {
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              val registrationCompleteNotification =
                fromJson[RegistrationComplete](
                  messages.head.message).get

              assertStored[StorageManifest](
                hybridBucket,
                hybridTable,
                registrationCompleteNotification.storageManifest.id.value,
                registrationCompleteNotification.storageManifest
              )

              inside(registrationCompleteNotification.storageManifest) {
                case StorageManifest(
                    bagId,
                    sourceIdentifier,
                    identifiers,
                    FileManifest(ChecksumAlgorithm("sha256"), bagDigestFiles),
                    TagManifest(ChecksumAlgorithm("sha256"), Nil),
                    List(digitalLocation),
                    _,
                    _,
                    _,
                    _) =>
                  bagId shouldBe BagId(bagLocation.bagPath.value)
                  sourceIdentifier shouldBe SourceIdentifier(
                    IdentifierType("source", "Label"),
                    value = "123"
                  )
                  identifiers shouldBe List(sourceIdentifier)
                  bagDigestFiles should have size 1

                  digitalLocation shouldBe DigitalLocation(
                    s"http://${storageBucket.name}.s3.amazonaws.com/${bagLocation.storagePath}/${bagLocation.bagPath.value}",
                    LocationType(
                      "aws-s3-standard-ia",
                      "AWS S3 Standard IA"
                    )
                  )
              }
          }
        }
        }

  }
}
