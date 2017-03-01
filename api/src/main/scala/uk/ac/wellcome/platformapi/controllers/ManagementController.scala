package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import uk.ac.wellcome.finatra.services.ElasticsearchService

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.Future
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManagementController @Inject()(
  elasticsearchService: ElasticsearchService
) extends Controller {

  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }

  get("/management/clusterhealth") { request: Request =>
    elasticsearchService.client
      .execute { clusterHealth() }
      .map(health => response.ok.json(health.status))
  }
}
