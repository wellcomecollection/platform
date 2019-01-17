package uk.ac.wellcome.platform.sierra_reader.flow

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.models.transformable.sierra.AbstractSierraRecord

object SierraRecordWrapperFlow {
  def apply[T <: AbstractSierraRecord](
    createRecord: (String, String, Instant) => T): Flow[Json, T, NotUsed] =
    Flow.fromFunction({ json =>
      val id = getId(json)
      val data = json.noSpaces
      val modifiedDate = getModifiedDate(json)

      createRecord(id, data, modifiedDate)
    })

  private def getModifiedDate(json: Json): Instant = {
    val maybeUpdatedDate = root.updatedDate.string.getOption(json)

    maybeUpdatedDate match {
      case Some(updatedDate) => Instant.parse(updatedDate)
      case None              => getDeletedDateTimeAtStartOfDay(json)
    }
  }

  private def getDeletedDateTimeAtStartOfDay(json: Json): Instant = {
    val formatter = DateTimeFormatter.ISO_DATE
    LocalDate
      .parse(root.deletedDate.string.getOption(json).get, formatter)
      .atStartOfDay()
      .toInstant(ZoneOffset.UTC)
  }

  private def getId(json: Json) =
    root.id.string.getOption(json).get
}
