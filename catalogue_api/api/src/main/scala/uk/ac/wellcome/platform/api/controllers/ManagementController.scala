package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.display.json.DisplayJsonUtil

import scala.concurrent.ExecutionContext

@Singleton
class ManagementController @Inject()(
  elasticClient: HttpClient
)(implicit ec: ExecutionContext)
    extends Controller {

  get("/management/healthcheck") { request: Request =>
    response.ok.body(
      bodyStr = DisplayJsonUtil.toJson("message" -> "ok")
    )
  }

  get("/management/clusterhealth") { request: Request =>
    elasticClient
      .execute { clusterHealth() }
      .map { health =>
        response.ok.body(bodyStr = DisplayJsonUtil.toJson(health.status))
      }
  }
}
