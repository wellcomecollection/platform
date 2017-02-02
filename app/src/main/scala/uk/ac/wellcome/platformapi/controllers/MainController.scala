package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.api.services._

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.Future
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import scala.concurrent.ExecutionContext.Implicits.global

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty


import uk.ac.wellcome.platform.api.models._

case class RecordRequest(
  @NotEmpty @QueryParam altRefNo: String
)

case class Response(
  refno: String,
  entries: Record,
  parent: Option[Collection]
)

@Singleton
class MainController @Inject()(
  calmService: CalmService
) extends Controller {

  val apiBaseUrl = "/api/v0"

  get(s"${apiBaseUrl}/record") { request: RecordRequest =>
    calmService.findRecordsByAltRefNo(request.altRefNo).map { records =>
      response.ok.json(records)
    }
  }

  get(s"${apiBaseUrl}/collection") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }


}
