package uk.ac.wellcome.platform.archive.progress_http

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressTrackerFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Namespace,
  Progress,
}
import uk.ac.wellcome.platform.archive.progress_http.fixtures.ProgressHttpFixture
import uk.ac.wellcome.storage.ObjectLocation

class ProgressHttpFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressTrackerFixture
    with ProgressHttpFixture
    with RandomThings
    with Inside
    with IntegrationPatience {

  import HttpMethods._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  import uk.ac.wellcome.storage.dynamo._
  import Progress._

  describe("GET /progress/:id") {
    it("returns a progress tracker when available") {
      withConfiguredApp {
        case (table, _, baseUrl, app) =>
          app.run()
          withActorSystem { actorSystem =>
            withMaterializer(actorSystem) { implicit materialiser =>
              withProgressTracker(table) { progressTracker =>
                val progress = createProgress()
                progressTracker.initialise(progress)
                val request =
                  HttpRequest(GET, s"$baseUrl/progress/${progress.id}")

                whenRequestReady(request) { result =>
                  result.status shouldBe StatusCodes.OK
                  getT[DisplayIngest](result.entity) shouldBe DisplayIngest(
                    progress)

                }
              }
            }
          }
      }
    }

    it("returns a 404 NotFound if no progress tracker matches id") {
      withConfiguredApp {
        case (_, _, baseUrl, app) =>
          app.run()

          withActorSystem { actorSystem =>
            implicit val system = actorSystem

            val uuid = randomUUID

            val request = Http().singleRequest(
              HttpRequest(GET, s"$baseUrl/progress/$uuid")
            )

            whenReady(request) { result: HttpResponse =>
              result.status shouldBe StatusCodes.NotFound
            }
          }
      }
    }
  }

  describe("POST /progress") {
    it("creates a progress tracker") {
      withConfiguredApp {
        case (table, topic, baseUrl, app) =>
          app.run()

          withActorSystem { actorSystem =>
            withMaterializer(actorSystem) { materializer =>
              implicit val system = actorSystem
              implicit val mat = materializer

              val url = s"$baseUrl/progress"
              val space = Namespace("space-id")

              val someCallback = Some(
                DisplayCallback(uri = testCallbackUri.toString, status = None))
              val storageSpace = DisplayStorageSpace(id = "somespace")
              val displayIngestType = DisplayIngestType(id = "create")
              val createProgressRequest = DisplayIngest(
                id = None,
                uploadUrl = testUploadUri.toString,
                callback = someCallback,
                space = storageSpace,
                ingestType = displayIngestType
              )

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                toJson(createProgressRequest).get
              )

              val httpRequest = HttpRequest(
                method = POST,
                uri = url,
                headers = Nil,
                entity = entity
              )

              val request = Http().singleRequest(httpRequest)

              val expectedLocationR = s"$url/(.+)".r

              whenReady(request) { result: HttpResponse =>

                result.status shouldBe StatusCodes.Created

                val maybeId = result.headers.collectFirst {
                  case HttpHeader("location", expectedLocationR(id)) => id
                }

                maybeId.isEmpty shouldBe false
                val id = UUID.fromString(maybeId.get)

                val progressFuture = Unmarshal(result.entity).to[DisplayIngest]

                whenReady(progressFuture) { actualProgress =>
                  val expectedProgress = DisplayIngest(
                    Some(id),
                    uploadUrl = testUploadUri.toString,
                    space = storageSpace,
                    ingestType = displayIngestType,
                    callback = Some(
                      DisplayCallback(testCallbackUri.toString, Some("pending")))
                  )

                  actualProgress shouldBe expectedProgress
                  assertTableOnlyHasItem(expectedProgress, table)

                  val requests =
                    listMessagesReceivedFromSNS(topic).map(messageInfo =>
                      fromJson[IngestBagRequest](messageInfo.message).get)

                  requests shouldBe List(IngestBagRequest(
                    id,
                    storageSpace = StorageSpace(space.underlying),
                    archiveCompleteCallbackUrl = Some(testCallbackUri),
                    zippedBagLocation =
                      ObjectLocation("ingest-bucket", "bag.zip")
                  ))
                }
              }
            }
          }
      }
    }
  }
}
