package uk.ac.wellcome.platform.archive.registrar.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.platform.archive.registrar.http.fixtures.RegistrarHttpFixture
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.storage.dynamo._

class RegistrarHttpFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with RegistrarHttpFixture
    with RandomThings
    with IntegrationPatience {

  import HttpMethods._
  import uk.ac.wellcome.json.JsonUtil._

  describe("GET /registrar/:space/:id") {
    it("returns a storage manifest when available") {
      withConfiguredApp {
        case (vhs, baseUrl, app) =>
          app.run()

          withActorSystem { implicit actorSystem =>
            withMaterializer(actorSystem) { implicit actorMaterializer =>
              val bagId = randomBagId

              val sourceIdentifier = SourceIdentifier(
                IdentifierType("source", "Label"),
                value = "123"
              )
              val storageManifest = StorageManifest(
                bagId,
                sourceIdentifier,
                Nil,
                FileManifest(ChecksumAlgorithm("sha256"), Nil),
                TagManifest(ChecksumAlgorithm("sha256"), Nil),
                Nil)
              val putResult = vhs.updateRecord(
                s"${storageManifest.id.space}/${storageManifest.id.externalIdentifier}")(
                ifNotExisting = (storageManifest, EmptyMetadata()))(
                ifExisting = (_,_) => fail("vhs should have been empty!"))
              whenReady(putResult) { _ =>

                val request = HttpRequest(
                  GET,
                  s"$baseUrl/registrar/${storageManifest.id.space.underlying}/${storageManifest.id.externalIdentifier.underlying}")

                whenRequestReady(request) { result =>
                  result.status shouldBe StatusCodes.OK
                  getT[StorageManifest](result.entity) shouldBe storageManifest

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
              HttpRequest(GET, s"$baseUrl/registrar/${bagId.space.underlying}/${bagId.externalIdentifier.underlying}")
            )

            whenReady(request) { result: HttpResponse =>
              result.status shouldBe StatusCodes.NotFound
            }
          }
      }
    }
  }
}
