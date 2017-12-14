package uk.ac.wellcome.sierra_adapter.sink

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import io.circe.Json
import io.circe.optics.JsonPath.root

object SierraDynamoSink {
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
