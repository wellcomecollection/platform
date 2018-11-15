package uk.ac.wellcome.platform.archive.registrar.http

import java.time.Instant
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Sink
import io.circe.optics.JsonPath._
import io.circe.parser._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.models.{
  StorageLocation,
  StorageProvider
}
import uk.ac.wellcome.platform.archive.display.{
  DisplayLocation,
  DisplayProvider,
  DisplayStorageSpace
}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.platform.archive.registrar.http.fixtures.RegistrarHttpFixture
import uk.ac.wellcome.platform.archive.registrar.http.models._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.EmptyMetadata

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
              val space = randomStorageSpace
              val bagInfo = randomBagInfo

              val checksumAlgorithm = "sha256"
              val path = "path"
              val bucket = "bucket"
              val providerId = "provider-id"
              val storageManifest = StorageManifest(
                space = space,
                info = bagInfo,
                manifest =
                  FileManifest(ChecksumAlgorithm(checksumAlgorithm), Nil),
                tagManifest = FileManifest(
                  ChecksumAlgorithm(checksumAlgorithm),
                  List(
                    BagDigestFile(Checksum("a"), BagFilePath("bag-info.txt")))),
                StorageLocation(
                  StorageProvider(providerId),
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
                        actualContextUrl,
                        actualBagId,
                        DisplayStorageSpace(storageSpaceName, "Space"),
                        DisplayBagInfo(
                          externalIdentifierString,
                          payloadOxum,
                          sourceOrganization,
                          baggingDate,
                          _,
                          _,
                          _,
                          "BagInfo"),
                        DisplayBagManifest(
                          actualDataManifestChecksumAlgorithm,
                          Nil,
                          "BagManifest"),
                        DisplayBagManifest(
                          actualTagManifestChecksumAlgorithm,
                          List(DisplayFileDigest("a", "bag-info.txt", "File")),
                          "BagManifest"),
                        DisplayLocation(
                          DisplayProvider(actualProviderId, "Provider"),
                          actualBucket,
                          actualPath,
                          "Location"),
                        createdDateString,
                        "Bag") =>
                      actualContextUrl shouldBe "http://api.wellcomecollection.org/storage/v1/context.json"
                      actualBagId shouldBe s"${space.underlying}/${bagInfo.externalIdentifier.underlying}"
                      storageSpaceName shouldBe space.underlying
                      externalIdentifierString shouldBe bagInfo.externalIdentifier.underlying
                      payloadOxum shouldBe s"${bagInfo.payloadOxum.payloadBytes}.${bagInfo.payloadOxum.numberOfPayloadFiles}"
                      sourceOrganization shouldBe bagInfo.sourceOrganisation.underlying
                      baggingDate shouldBe bagInfo.baggingDate.format(
                        DateTimeFormatter.ISO_LOCAL_DATE)

                      actualDataManifestChecksumAlgorithm shouldBe checksumAlgorithm
                      actualTagManifestChecksumAlgorithm shouldBe checksumAlgorithm
                      actualProviderId shouldBe providerId
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
    it("does not output null values") {
      withConfiguredApp {
        case (vhs, baseUrl, app) =>
          app.run()

          withActorSystem { implicit actorSystem =>
            withMaterializer(actorSystem) { implicit actorMaterializer =>
              val space = randomStorageSpace
              val bagInfo = randomBagInfo.copy(externalDescription = None)

              val checksumAlgorithm = "sha256"
              val path = "path"
              val bucket = "bucket"
              val providerId = "provider-id"
              val storageManifest = StorageManifest(
                space = space,
                info = bagInfo,
                manifest =
                  FileManifest(ChecksumAlgorithm(checksumAlgorithm), Nil),
                tagManifest = FileManifest(
                  ChecksumAlgorithm(checksumAlgorithm),
                  List(
                    BagDigestFile(Checksum("a"), BagFilePath("bag-info.txt")))),
                StorageLocation(
                  StorageProvider(providerId),
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
                  val value = result.entity.dataBytes.runWith(Sink.fold("") {
                    case (acc, byteString) =>
                      acc + byteString.utf8String
                  })
                  whenReady(value) { jsonString =>
                    val infoJson =
                      root.info.json.getOption(parse(jsonString).right.get).get
                    infoJson.findAllByKey("externalDescription") shouldBe empty
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
