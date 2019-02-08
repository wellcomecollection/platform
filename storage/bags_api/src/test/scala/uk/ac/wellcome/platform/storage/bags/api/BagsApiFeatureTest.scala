package uk.ac.wellcome.platform.storage.bags.api

import java.time.Instant
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model._
import io.circe.optics.JsonPath._
import io.circe.parser._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.generators.BagInfoGenerators
import uk.ac.wellcome.platform.archive.common.http.HttpMetricResults
import uk.ac.wellcome.platform.archive.display.{
  DisplayLocation,
  DisplayStorageSpace,
  StandardDisplayProvider
}
import uk.ac.wellcome.platform.archive.registrar.generators.StorageManifestGenerators
import uk.ac.wellcome.platform.storage.bags.api.fixtures.BagsApiFixture
import uk.ac.wellcome.platform.storage.bags.api.models.{
  DisplayBag,
  DisplayBagInfo,
  DisplayBagManifest,
  DisplayFileDigest
}
import uk.ac.wellcome.storage.ObjectLocation

class BagsApiFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with BagInfoGenerators
    with BagsApiFixture
    with RandomThings
    with IntegrationPatience
    with Inside
    with StorageManifestGenerators {

  import uk.ac.wellcome.json.JsonUtil._

  describe("GET /registrar/:space/:id") {
    it("returns a bag when available") {
      withConfiguredApp {
        case (vhs, metricsSender, baseUrl) =>
          withMaterializer { implicit materializer =>
            val space = randomStorageSpace
            val bagInfo = randomBagInfo

            val checksumAlgorithm = "sha256"
            val path = "path"
            val bucket = "bucket"
            val archiveBucket = "archive-bucket"
            val archivePath = "archive-path"
            val storageManifest = createStorageManifestWith(
              space = space,
              bagInfo = bagInfo,
              checksumAlgorithm = checksumAlgorithm,
              accessLocation = ObjectLocation(namespace = bucket, key = path),
              archiveLocations =
                List(ObjectLocation(archiveBucket, archivePath))
            )
            val future = storeSingleManifest(vhs, storageManifest)
            whenReady(future) { _ =>
              whenGetRequestReady(
                s"$baseUrl/registrar/${storageManifest.id.space.underlying}/${storageManifest.id.externalIdentifier.underlying}") {
                response =>
                  response.status shouldBe StatusCodes.OK
                  val displayBag = getT[DisplayBag](response.entity)

                  inside(displayBag) {
                    case DisplayBag(
                        actualContextUrl,
                        actualBagId,
                        DisplayStorageSpace(storageSpaceName, "Space"),
                        DisplayBagInfo(
                          externalIdentifierString,
                          payloadOxum,
                          baggingDate,
                          sourceOrganization,
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
                          StandardDisplayProvider,
                          actualBucket,
                          actualPath,
                          "Location"),
                        List(
                          DisplayLocation(
                            StandardDisplayProvider,
                            archiveBucket,
                            archivePath,
                            "Location")),
                        createdDateString,
                        "Bag") =>
                      actualContextUrl shouldBe "http://api.wellcomecollection.org/storage/v1/context.json"
                      actualBagId shouldBe s"${space.underlying}/${bagInfo.externalIdentifier.underlying}"
                      storageSpaceName shouldBe space.underlying
                      externalIdentifierString shouldBe bagInfo.externalIdentifier.underlying
                      payloadOxum shouldBe s"${bagInfo.payloadOxum.payloadBytes}.${bagInfo.payloadOxum.numberOfPayloadFiles}"
                      sourceOrganization shouldBe bagInfo.sourceOrganisation.map(_.underlying)
                      baggingDate shouldBe bagInfo.baggingDate.format(
                        DateTimeFormatter.ISO_LOCAL_DATE)

                      actualDataManifestChecksumAlgorithm shouldBe checksumAlgorithm
                      actualTagManifestChecksumAlgorithm shouldBe checksumAlgorithm

                      actualBucket shouldBe bucket
                      actualPath shouldBe path

                      archiveBucket shouldBe archiveBucket
                      archivePath shouldBe archivePath

                      Instant.parse(createdDateString) shouldBe storageManifest.createdDate
                  }

                  assertMetricSent(
                    metricsSender,
                    result = HttpMetricResults.Success)
              }
            }
          }
      }
    }

    it("does not output null values") {
      withConfiguredApp {
        case (vhs, metricsSender, baseUrl) =>
          withMaterializer { implicit materializer =>
            val storageManifest = createStorageManifestWith(
              bagInfo = createBagInfoWith(externalDescription = None)
            )
            val future = storeSingleManifest(vhs, storageManifest)
            whenReady(future) { _ =>
              whenGetRequestReady(
                s"$baseUrl/registrar/${storageManifest.id.space.underlying}/${storageManifest.id.externalIdentifier.underlying}") {
                response =>
                  response.status shouldBe StatusCodes.OK

                  withStringEntity(response.entity) { jsonString =>
                    val infoJson =
                      root.info.json
                        .getOption(parse(jsonString).right.get)
                        .get
                    infoJson.findAllByKey("externalDescription") shouldBe empty
                  }

                  assertMetricSent(
                    metricsSender,
                    result = HttpMetricResults.Success)
              }
            }
          }
      }
    }

    it("returns a 404 NotFound if no progress monitor matches id") {
      withConfiguredApp {
        case (_, metricsSender, baseUrl) =>
          val bagId = randomBagId
          whenGetRequestReady(
            s"$baseUrl/registrar/${bagId.space.underlying}/${bagId.externalIdentifier.underlying}") {
            response =>
              response.status shouldBe StatusCodes.NotFound

              assertMetricSent(
                metricsSender,
                result = HttpMetricResults.UserError)
          }
      }
    }

    it("returns a 500 error if looking up the bag fails") {
      withBrokenApp {
        case (_, metricsSender, baseUrl) =>
          val bagId = randomBagId
          whenGetRequestReady(
            s"$baseUrl/registrar/${bagId.space.underlying}/${bagId.externalIdentifier.underlying}") {
            response =>
              response.status shouldBe StatusCodes.InternalServerError

              assertMetricSent(
                metricsSender,
                result = HttpMetricResults.ServerError)
          }
      }
    }
  }
}
