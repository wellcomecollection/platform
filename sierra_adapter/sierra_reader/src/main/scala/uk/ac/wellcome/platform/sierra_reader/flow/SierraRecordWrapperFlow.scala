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
    Flow.fromFunction({ json => createSierraRecord(json)})

  private def createSierraRecord(json: Json): SierraRecord = {
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

  private def getId(json: Json) = {
    root.id.string.getOption(json).get
  }
}
