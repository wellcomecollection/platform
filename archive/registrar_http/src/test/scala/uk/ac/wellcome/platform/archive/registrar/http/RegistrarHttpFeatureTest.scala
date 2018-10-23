package uk.ac.wellcome.platform.archive.registrar.http

import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.DisplayStorageSpace
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.platform.archive.registrar.http.fixtures.RegistrarHttpFixture
import uk.ac.wellcome.platform.archive.registrar.http.models._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.storage.dynamo._

class RegistrarHttpFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with RegistrarHttpFixture
    with RandomThings
    with IntegrationPatience
    with Inside {

  import HttpMethods._
  import uk.ac.wellcome.json.JsonUtil._

  describe("GET /registrar/:space/:id") {
    it("returns a bag when available") {
      withConfiguredApp {
        case (vhs, baseUrl, app) =>
          app.run()

          withActorSystem { implicit actorSystem =>
            withMaterializer(actorSystem) { implicit actorMaterializer =>
              val bagId = randomBagId

              val checksumAlgorithm = "sha256"
              val path = "path"
              val bucket = "bucket"
              val providerId = "provider-id"
              val providerLabel = "provider label"
              val storageManifest = StorageManifest(
                id = bagId,
                manifest =
                  FileManifest(ChecksumAlgorithm(checksumAlgorithm), Nil),
                Location(
                  Provider(providerId, providerLabel),
                  ObjectLocation(bucket, path)),
                Instant.now
              )
              val putResult = vhs.updateRecord(
                s"${storageManifest.id.space}/${storageManifest.id.externalIdentifier}")(
                ifNotExisting = (storageManifest, EmptyMetadata()))(ifExisting =
                (_, _) => fail("vhs should have been empty!"))
              whenReady(putResult) { _ =>
                val request = HttpRequest(
                  GET,
                  s"$baseUrl/registrar/${storageManifest.id.space.underlying}/${storageManifest.id.externalIdentifier.underlying}")

                whenRequestReady(request) { result =>
                  result.status shouldBe StatusCodes.OK
                  val displayBag = getT[DisplayBag](result.entity)

                  inside(displayBag) {
                    case DisplayBag(
                        actualBagId,
                        DisplayStorageSpace(storageSpaceName, "Space"),
                        DisplayBagInfo(externalIdentifierString, "BagInfo"),
                        DisplayBagManifest(
                          actualChecksumAlgorithm,
                          Nil,
                          "BagManifest"),
                        DisplayLocation(
                          DisplayProvider(
                            actualProviderId,
                            actualProviderLabel,
                            "Provider"),
                          actualBucket,
                          actualPath,
                          "Location"),
                        createdDateString,
                        "Bag") =>
                      actualBagId shouldBe s"${bagId.space.underlying}/${bagId.externalIdentifier.underlying}"
                      storageSpaceName shouldBe bagId.space.underlying
                      externalIdentifierString shouldBe bagId.externalIdentifier.underlying
                      actualChecksumAlgorithm shouldBe checksumAlgorithm
                      actualProviderId shouldBe providerId
                      actualProviderLabel shouldBe providerLabel
                      actualBucket shouldBe bucket
                      actualPath shouldBe path

                      Instant.parse(createdDateString) shouldBe storageManifest.createdDate
                  }

                }
              }
            }
          }
      }
    }

    it("returns a 404 NotFound if no progress monitor matches id") {
      withConfiguredApp {
        case (_, baseUrl, app) =>
          app.run()

          withActorSystem { implicit actorSystem =>
            val bagId = randomBagId
            val request = Http().singleRequest(
              HttpRequest(
                GET,
                s"$baseUrl/registrar/${bagId.space.underlying}/${bagId.externalIdentifier.underlying}")
            )

            whenReady(request) { result: HttpResponse =>
              result.status shouldBe StatusCodes.NotFound
            }
          }
      }
    }
  }
}
