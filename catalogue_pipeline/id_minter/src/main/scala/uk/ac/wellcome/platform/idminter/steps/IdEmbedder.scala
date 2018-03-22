package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.optics.JsonPath.root
import io.circe.optics.JsonTraversalPath
import io.circe.{Json, _}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{Failure, Success}
import uk.ac.wellcome.models.Identified

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
  private def iterate(rootChildrenPath: JsonTraversalPath, json: Json): Json = {
    if (rootChildrenPath.json.nonEmpty(json)) {
      val newJson =
        rootChildrenPath.json.modify(addIdentifierToJson)(json)
      iterate(rootChildrenPath.each, newJson)
    } else json
  }

  private def addIdentifierToJson(json: Json): Json = {
    if (json.isObject) {
      json.mapObject(obj => addIdentifierToJsonObject(obj))
    } else json
  }

  private def addIdentifierToJsonObject(obj: JsonObject): JsonObject = {
    if (obj.contains("sourceIdentifier")) {
      val sourceIdentifier = parseSourceIdentifier(obj)

      val canonicalId = identifierGenerator
        .retrieveOrGenerateCanonicalId(
          identifier = sourceIdentifier
        )
        .get

      if (obj.contains("type")) {
        (("type", Json.fromString("Identified")) +:
          ("canonicalId", Json.fromString(canonicalId)) +: obj)
          .remove("sourceIdentifier")
      } else {
        ("canonicalId", Json.fromString(canonicalId)) +: obj
      }

    } else obj
  }

  private def parseSourceIdentifier(obj: JsonObject): SourceIdentifier = {
    fromJson[SourceIdentifier](obj("sourceIdentifier").get.toString()) match {
      case Success(sourceIdentifier) => sourceIdentifier
      case Failure(exception) =>
        error(s"Error parsing source identifier: $exception")
        throw exception
    }

  }
}
