package uk.ac.wellcome.platform.archive.progress_http

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressCreateRequest
}
import uk.ac.wellcome.platform.archive.progress_http.fixtures.ProgressHttpFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

class ProgressHttpFeatureTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressHttpFixture
    with ExtendedPatience {

  import HttpMethods._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  describe("GET /progress/:id") {
    it("returns a progress monitor when available") {
      withConfiguredApp {
        case (table, baseUrl, app) =>
          app.run()

          withActorSystem { actorSystem =>
            implicit val system = actorSystem
            implicit val actorMaterializer = ActorMaterializer()

            withProgressMonitor(table) { progressMonitor =>
              val progress = createProgress(progressMonitor)
              val request =
                HttpRequest(GET, s"$baseUrl/progress/${progress.id}")

              whenRequestReady(request) { result =>
                result.status shouldBe StatusCodes.OK
                getT[Progress](result.entity) shouldBe progress
              }
            }
          }
      }
    }

    it("returns a 404 NotFound if no progress monitor matches id") {
      withConfiguredApp {
        case (_, baseUrl, app) =>
          app.run()

          withActorSystem { actorSystem =>
            implicit val system = actorSystem

            val uuid = UUID.randomUUID().toString

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
    it("creates a progress monitor") {
      withConfiguredApp {
        case (table, baseUrl, app) =>
          app.run()

          withActorSystem { actorSystem =>
            withMaterializer(actorSystem) { materializer =>
              implicit val system = actorSystem
              implicit val mat = materializer

              val url = s"$baseUrl/progress"

              val createProgressRequest = ProgressCreateRequest(
                uploadUrl,
                Some(callbackUrl)
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
                // Successful request
                result.status shouldBe StatusCodes.Created

                debug(result)

                // Collect first location header matching pattern
                val maybeId = result.headers.collectFirst {
                  case HttpHeader("location", expectedLocationR(id)) => id
                }

                // We should have an id
                maybeId.isEmpty shouldBe false
                val id = maybeId.get

                // Check progress is returned
                val progressFuture = Unmarshal(result.entity).to[Progress]

                // Check the progress is stored
                whenReady(progressFuture) { progress =>
                  val expectedProgress = Progress(
                    id,
                    uploadUrl,
                    Some(callbackUrl)
                  ).copy(
                    createdAt = progress.createdAt,
                    updatedAt = progress.updatedAt
                  )

                  progress shouldBe expectedProgress
                  assertTableOnlyHasItem(expectedProgress, table)
                }
              }
            }
          }
      }
    }
  }
}
