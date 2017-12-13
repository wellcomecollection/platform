package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{MergedSierraRecord, SierraBibRecord}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraBibMergerUpdaterService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metrics: MetricsSender,
  dynamoConfig: DynamoConfig
) extends Logging {

  private val table = Table[MergedSierraRecord](dynamoConfig.table)

  private def putRecord(record: MergedSierraRecord) =
    table
      .given(
        not(attributeExists('id)) or
          (attributeExists('id) and 'version < record.version)
      )
      .put(record)

  def update(bibRecord: SierraBibRecord): Future[Unit] = Future {
    logger.info(s"Attempting to update $bibRecord")

    val existingRecord = Scanamo.exec(dynamoDBClient)(
      table.get('id -> bibRecord.id)
    )

    existingRecord
      .map {
        case Left(error) =>
          throw new RuntimeException(error.toString)
        case Right(record) => {
          logger.info(s"Found $record, attempting merge.")

          val newRecord = record.mergeBibRecord(bibRecord)
          if (record != newRecord) {
            writeRecordToDynamo(newRecord)
          }
        }
      }
      .getOrElse {
        val record = MergedSierraRecord(bibRecord)
        logger.info(s"No match found, creating new record: $record")
        writeRecordToDynamo(record)
      }
  }

  private def writeRecordToDynamo(record: MergedSierraRecord) {
    val recordToWrite = record.copy(version = record.version + 1)
    logger.info(s"Attempting to conditionally update $recordToWrite.")

    val putOperation = Scanamo
      .exec(dynamoDBClient)(
        putRecord(newRecord)
      )
      .left
      .map(e => new RuntimeException(e.toString))

    putOperation match {
      case Right(_) =>
        logger.info(s"${record.id} saved successfully to DynamoDB")
      case Left(error) =>
        logger.warn(s"Failed processing ${record.id}", error)
    }
  }
}
