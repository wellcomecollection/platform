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
  extends Controller {

  prefix(apiPrefix) {

    get(s"/record") { request: CalmRequest =>
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
