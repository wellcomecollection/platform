package uk.ac.wellcome.platform.sierra_to_dynamo.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import uk.ac.wellcome.sqs.SQSReaderGracefulException

import scala.util.Try

object WindowExtractor extends Logging {
  def extractWindow(jsonString: String): Try[String] =
    Try(parse(jsonString).right.get)
      .map { json =>
        val start = extractField("start", json)
        val end = extractField("end", json)

        val startDateTime = parseStringToDateTime(start)
        val endDateTime = parseStringToDateTime(end)
        if (startDateTime.isAfter(endDateTime) || startDateTime.isEqual(
              endDateTime))
          throw new Exception(s"$start is after $end")

        s"[$start,$end]"
      }
      .recover {
        case e: Exception =>
          warn(s"Error parsing $jsonString", e)
          throw SQSReaderGracefulException(e)
      }

  private def extractField(field: String, json: Json) = {
    root.selectDynamic(field).string.getOption(json).get
  }

  private def parseStringToDateTime(start: String) = {
    val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    LocalDateTime.parse(start, formatter)
  }
}
