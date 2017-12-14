package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.utils.GlobalExecutionContext.context

@Singleton
class ClusterHealthController @Inject()(
  elasticClient: HttpClient
) extends Controller {
  get("/management/clusterhealth") { request: Request =>
    elasticClient
      .execute { clusterHealth() }
      .map(health => response.ok.json(health.status))
  }
}
