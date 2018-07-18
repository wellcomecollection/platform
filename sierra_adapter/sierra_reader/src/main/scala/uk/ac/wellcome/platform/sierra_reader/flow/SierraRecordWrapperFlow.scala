package uk.ac.wellcome.platform.sierra_reader.flow

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

object SierraRecordWrapperFlow extends Logging {
  def apply(): Flow[Json, SierraRecord, NotUsed] =
    Flow.fromFunction({ json =>
      createSierraRecord(json)
    })

  private def createSierraRecord(json: Json): SierraRecord = {
    logger.debug(s"Creating record from ${json.noSpaces}")
    val maybeUpdatedDate = root.updatedDate.string.getOption(json)

    val modifiedDate: Instant = maybeUpdatedDate match {
      case Some(updatedDate) => Instant.parse(updatedDate)
      case None              => getDeletedDateTimeAtStartOfDay(json)
    }

    SierraRecord(
      id = SierraRecordNumber(getId(json)),
      data = json.noSpaces,
      modifiedDate = modifiedDate
    )
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
