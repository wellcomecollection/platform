package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.Future

class IdEmbedder @Inject()(metricsSender: MetricsSender,
                           identifierGenerator: IdentifierGenerator)
    extends Logging {
  def embedId(json: Json): Future[Json] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        Future {
          val newJson = json.mapObject(addIdentifierToJsonObject)
          root.items.each.json.modify(addIdentifierToJson)(newJson)
      }
    )
  }

  private def addIdentifierToJson(json: Json) = {
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
