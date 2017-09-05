package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Json, _}
import io.circe.generic.auto._
import io.circe.optics.JsonPath.root
import io.circe.optics.JsonTraversalPath
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.annotation.tailrec
import scala.concurrent.Future

class IdEmbedder @Inject()(metricsSender: MetricsSender,
                           identifierGenerator: IdentifierGenerator)
    extends Logging {
  def embedId(json: Json): Future[Json] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        Future {
          iterate(root.each, addIdentifierToJson(json))
      }
    )
  }
  
  @tailrec
  private def iterate(rootChildrenPath: JsonTraversalPath, json: Json):Json = {
    if (rootChildrenPath.json.nonEmpty(json)) {
      val newJson =
        rootChildrenPath.json.modify(addIdentifierToJson)(json)
      iterate(rootChildrenPath.each, newJson)
    }
    else json
  }

  private def addIdentifierToJson(json: Json):Json = {
    if (json.isObject) {
      json.mapObject(obj =>
        addIdentifierToJsonObject(obj))
    }
    else json
  }

  private def addIdentifierToJsonObject(obj: JsonObject) = {
    if (obj.contains("identifiers")) {
      val canonicalId = identifierGenerator
        .retrieveOrGenerateCanonicalId(
          decode[List[SourceIdentifier]](obj("identifiers").get.toString()).right.get,
          ontologyType = obj("ontologyType").get.toString()
        )
        .get
      ("canonicalId", Json.fromString(canonicalId)) +: obj
    }
    else obj
  }
}
