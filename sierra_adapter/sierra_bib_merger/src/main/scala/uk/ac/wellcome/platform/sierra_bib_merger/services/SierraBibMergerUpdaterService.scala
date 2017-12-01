package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.google.inject.Inject
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.syntax._
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{MergedSierraRecord, SierraBibRecord}
import uk.ac.wellcome.models.SierraBibRecord._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metrics: MetricsSender,
  dynamoConfig: DynamoConfig
) extends Logging {

  def update(bibRecord: SierraBibRecord): Unit = {

    // First we read the existing record from the table
    val existingRecord: MergedSierraRecord = Scanamo.get[MergedSierraRecord](
      dynamoDBClient)(dynamoConfig.table)('id -> bibRecord.id) match {

      // TODO: Handle this properly!
      case Some(record) => record.right.get
      case None => MergedSierraRecord(id = bibRecord.id)
    }

    // Then we add the record we've received from the Sierra API, and
    // optionally send that to DynamoDB.
    val newRecord: Option[MergedSierraRecord] =
      existingRecord.mergeBibRecord(bibRecord)

    newRecord match {
      case Some(record) => {
        val table = Table[MergedSierraRecord](dynamoConfig.table)
        val ops = table
          .given(
            not(attributeExists('id)) or
              (attributeExists('id) and 'version < record.version)
          )
          .put(record)
        val x = Scanamo.exec(dynamoDBClient)(ops) match {
          case Right(_) =>
            logger.info(s"$record saved successfully to DynamoDB")
          case Left(error) =>
            logger.warn(s"Failed saving $record to DynamoDB", error)
        }
      }
      case None => ()
    }
  }
}
