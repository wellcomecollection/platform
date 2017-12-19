package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.generic.auto._
import io.circe.optics.JsonPath.root
import io.circe.optics.JsonTraversalPath
import io.circe.parser._
import io.circe.{Json, _}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
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
      val ontologyType = obj("type").get.asString.get

      val canonicalId = identifierGenerator
        .retrieveOrGenerateCanonicalId(
          identifier = sourceIdentifier,
          ontologyType = ontologyType
        )
        .get
      ("canonicalId", Json.fromString(canonicalId)) +: obj
    } else obj
  }

  private def parseSourceIdentifier(obj: JsonObject): SourceIdentifier = {
    decode[SourceIdentifier](obj("sourceIdentifier").get.toString()) match {
      case Right(sourceIdentifier) => sourceIdentifier
      case Left(exception: Error) =>
        error(s"Error parsing source identifier: $exception")
        throw exception
    }

  }
}
