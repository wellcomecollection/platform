package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import scala.concurrent.ExecutionContext

@Singleton
class ManagementController @Inject()(
  elasticClient: ElasticClient
)(implicit ec: ExecutionContext)
    extends Controller {

  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }

  get("/management/clusterhealth") { request: Request =>
    elasticClient
      .execute { clusterHealth() }
      .map(health => response.ok.json(health.status))
  }
}
