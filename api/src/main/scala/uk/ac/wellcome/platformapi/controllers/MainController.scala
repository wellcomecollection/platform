package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.api.services._
import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.Future
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.platform.api.ApiSwagger
import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.inject.annotations.Flag


case class CalmRequest(
  @NotEmpty @QueryParam altRefNo: String
)

case class RecordCollectionPair(
  record: Option[Record],
  collection: Option[Collection]
)

case class RecordResponse(
  altRefNo: String,
  entries: Record,
  parent: Option[Collection]
)

@Singleton
class MainController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  calmService: CalmService
)
  extends Controller
  with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  prefix(apiPrefix) {

    getWithDoc(s"/record") { o =>
      o.summary("Read record information")
        .description("Read the record information!")
        .produces("application/json")
        .tag("API")
        .queryParam[String]("altRefNo", "The record to return", required = true)
        .responseWith[Object](200, "A Record")
        .responseWith[Object](404, "The Record is not found")
    } { request: CalmRequest =>
      val recordCollectionPair = for {
        recordOption <- calmService.findRecordByAltRefNo(request.altRefNo)
        collectionOption <- calmService.findParentCollectionByAltRefNo(request.altRefNo)
      } yield RecordCollectionPair(recordOption, collectionOption)

      recordCollectionPair.map { pair =>
        pair.record
          .map(record =>
            response.ok.json(RecordResponse(request.altRefNo, record, pair.collection)))
          .getOrElse(response.notFound)
      }
    }

    get(s"/collection") { request: CalmRequest =>
      calmService.findCollectionByAltRefNo(request.altRefNo).map { collectionOption =>
        collectionOption
          .map(response.ok.json)
          .getOrElse(response.notFound)
      }
    }

  }
}
