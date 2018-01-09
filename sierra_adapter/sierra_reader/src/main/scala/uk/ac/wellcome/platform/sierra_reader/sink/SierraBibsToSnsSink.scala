package uk.ac.wellcome.platform.sierra_reader.sink

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import akka.Done
import akka.stream.scaladsl.Sink
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.{ExecutionContext, Future}

object SierraBibsToSnsSink extends Logging {
  def apply(writer: SNSWriter)(
    implicit executionContext: ExecutionContext): Sink[Json, Future[Done]] =
    Sink.foreachParallel(10)(unprefixedJson => {
      val json = addIDPrefix(json = unprefixedJson)
      logger.info(s"Sending ${json.noSpaces} to SNS")
      val maybeUpdatedDate = root.updatedDate.string.getOption(json)
      val record = maybeUpdatedDate match {
        case Some(updatedDate) =>
          SierraBibRecord(
            id = getId(json),
            data = json.noSpaces,
            modifiedDate = updatedDate
          )
        case None =>
          SierraBibRecord(
            id = getId(json),
            data = json.noSpaces,
            modifiedDate = getDeletedDateTimeAtStartOfDay(json)
          )
      }

      writer.writeMessage(
        message = JsonUtil.toJson(record).get,
        subject = Some("New bib record from Sierra")
      )
    })

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
  def addIDPrefix(json: Json): Json =
    root.id.string.modify(id => s"b$id")(json)

  private def getId(json: Json) = {
    root.id.string.getOption(json).get
  }
}
