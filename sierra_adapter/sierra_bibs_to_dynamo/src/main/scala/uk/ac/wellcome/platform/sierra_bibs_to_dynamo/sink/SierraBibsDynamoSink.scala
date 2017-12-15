package uk.ac.wellcome.platform.sierra_bibs_to_dynamo.sink

import akka.Done
import akka.stream.scaladsl.Sink
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import uk.ac.wellcome.models.SierraBibRecord
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.sierra_adapter.sink.SierraDynamoSink

import scala.concurrent.{ExecutionContext, Future}

object SierraBibsDynamoSink extends SierraDynamoSink with Logging {
  def apply(client: AmazonDynamoDB, tableName: String)(
    implicit executionContext: ExecutionContext): Sink[Json, Future[Done]] =
    Sink.foreachParallel(10)(unprefixedJson => {
      val json = addIDPrefix(json = unprefixedJson)
      logger.info(s"Inserting ${json.noSpaces} into DynamoDB")
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

      val table = Table[SierraBibRecord](tableName)
      val ops = table
        .given(
          not(attributeExists('id)) or
            (attributeExists('id) and 'modifiedDate < record.modifiedDate.getEpochSecond)
        )
        .put(record)
      Scanamo.exec(client)(ops) match {
        case Right(_) =>
          logger.info(s"${json.noSpaces} saved successfully to DynamoDB")
        case Left(error) =>
          logger.warn(s"Failed saving ${json.noSpaces} into DynamoDB", error)
      }
    })

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
}
