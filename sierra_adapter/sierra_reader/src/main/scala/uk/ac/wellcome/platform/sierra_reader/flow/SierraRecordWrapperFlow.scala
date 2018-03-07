package uk.ac.wellcome.platform.sierra_reader.flow

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.models.transformable.sierra.SierraRecord

import scala.concurrent.ExecutionContext

object SierraResourceTypes extends Enumeration {
  val bibs, items = Value
}

object SierraRecordWrapperFlow extends Logging {
  def apply(resourceType: SierraResourceTypes.Value)(
    implicit executionContext: ExecutionContext)
    : Flow[Json, SierraRecord, NotUsed] =
    Flow.fromFunction({ json =>
      createSierraRecord(json, resourceType)
    })

  private def createSierraRecord(
    unprefixedJson: Json,
    resourceType: SierraResourceTypes.Value): SierraRecord = {
    val json = addIDPrefix(
      json = unprefixedJson,
      resourceType: SierraResourceTypes.Value)
    logger.debug(s"Creating record from ${json.noSpaces}")
    val maybeUpdatedDate = root.updatedDate.string.getOption(json)
    maybeUpdatedDate match {
      case Some(updatedDate) =>
        SierraRecord(
          id = getId(json),
          data = json.noSpaces,
          modifiedDate = updatedDate
        )
      case None =>
        SierraRecord(
          id = getId(json),
          data = json.noSpaces,
          modifiedDate = getDeletedDateTimeAtStartOfDay(json)
        )
    }
  }

  private def getDeletedDateTimeAtStartOfDay(json: Json) = {
    val formatter = DateTimeFormatter.ISO_DATE
    LocalDate
      .parse(root.deletedDate.string.getOption(json).get, formatter)
      .atStartOfDay()
      .toInstant(ZoneOffset.UTC)
  }

  // Sierra assigns IDs for bibs and items in the same namespace.  A record
  // with ID "1234567" could be a bib or an item (or something else!).
  //
  // Outside Sierra, IDs are prefixed with a little to denote what type of
  // record they are, e.g. "b1234567" and "i1234567" refer to a bib and item,
  // respectively.
  //
  // This updates the ID in a block of JSON to add this disambiguating prefix.
  private def addIDPrefix(json: Json,
                          resourceType: SierraResourceTypes.Value): Json = {
    resourceType match {
      case SierraResourceTypes.bibs =>
        root.id.string.modify(id => s"b$id")(json)
      case SierraResourceTypes.items => {
        val identifiedJson = root.id.string.modify(id => s"i$id")(json)
        root.bibIds.each.string.modify(id => s"b$id")(identifiedJson)
      }
    }
  }

  private def getId(json: Json) = {
    root.id.string.getOption(json).get
  }
}
