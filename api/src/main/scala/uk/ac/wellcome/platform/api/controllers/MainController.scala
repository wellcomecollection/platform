package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.platform.api.services._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

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
) extends Controller {

  prefix(apiPrefix) {

    // This is a demo endpoint for the UX team to use when prototyping
    // item pages.
    // TODO: Remove this endpoint.
    get(s"/demoItem") {
      request: Request =>
        response.ok.json(Map(
          "@context" -> "http://id.wellcomecollection.org/",
          "id" -> "cbsx6cvr",
          "type" -> "item",
          "title" -> "The natural history of monkeys",
          "date" -> "1546-04-07",
          "authors" -> Array("William Jardine"),
          "description" -> "230 page, color plates : frontispiece (portrait), add. color t.page ; (8vo)",
          "topics" -> Array("monkeys", "animals"),
          "media" -> Array("http://wellcomelibrary.org/content/59301/60865")
        ))
    }

    get(s"/record") { request: CalmRequest =>
      val recordCollectionPair = for {
        recordOption <- calmService.findRecordByAltRefNo(request.altRefNo)
        collectionOption <- calmService.findParentCollectionByAltRefNo(
          request.altRefNo)
      } yield RecordCollectionPair(recordOption, collectionOption)

      recordCollectionPair.map { pair =>
        pair.record
          .map(record =>
            response.ok.json(
              RecordResponse(request.altRefNo, record, pair.collection)))
          .getOrElse(response.notFound)
      }
    }

    get(s"/collection") { request: CalmRequest =>
      calmService.findCollectionByAltRefNo(request.altRefNo).map {
        collectionOption =>
          collectionOption
            .map(response.ok.json)
            .getOrElse(response.notFound)
      }
    }

  }
}
