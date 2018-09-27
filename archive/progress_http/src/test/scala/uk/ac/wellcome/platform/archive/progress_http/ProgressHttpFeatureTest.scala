package uk.ac.wellcome.platform.archive.progress_http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.progress_http.fixtures.ProgressHttpFixture
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

class ProgressHttpFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ProgressHttpFixture
    with ExtendedPatience {

  describe("GET /progress/:id") {
    it("returns a progress monitor when available") {
      withConfiguredApp { case (table, baseUrl, app) =>

        app.run()

        withActorSystem { actorSystem =>

          implicit val system = actorSystem
          implicit val actorMaterializer = ActorMaterializer()

          withProgressMonitor(table) { progressMonitor =>
            val progress =
              createProgress(uploadUrl, callbackUrl, progressMonitor)

            assertTableOnlyHasItem(progress, table)

            val uri = s"$baseUrl/progress/${progress.id}"

            val request = Http().singleRequest(
              HttpRequest(uri = uri)
            )

            whenReady(request) { result: HttpResponse =>
              result.status shouldBe StatusCodes.OK
              getT[Progress](result.entity) shouldBe progress
            }
          }
        }
      }
    }

    it("returns a 404 NotFound if no progress monitor matches id") {
      withConfiguredApp { case (_, baseUrl, app) =>

        app.run()

        withActorSystem(actorSystem => {

          implicit val system = actorSystem

          val uri = s"$baseUrl/progress/fake_id"

          val request = Http().singleRequest(
            HttpRequest(uri = uri)
          )

          whenReady(request) { result: HttpResponse =>
            result.status shouldBe StatusCodes.NotFound
          }

        })
      }
    }
  }

  describe("POST /progress") {
    it("creates a progress monitor") {
      true shouldBe false
    }
  }
}

