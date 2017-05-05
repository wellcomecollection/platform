package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.utils.GlobalExecutionContext.context

@Singleton
class ManagementController @Inject()(
  elasticClient: TcpClient
) extends Controller {

  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }

  get("/management/clusterhealth") { request: Request =>
    elasticClient
      .execute { clusterHealth() }
      .map(health => response.ok.json(health.status))
  }
}
