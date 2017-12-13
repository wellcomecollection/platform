package uk.ac.wellcome.platform.sierra_item_merger.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
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

  def update(itemRecord: SierraItemRecord): Future[Unit] = Future {

    Scanamo.get[MergedSierraRecord](dynamoDBClient)(dynamoConfig.table)(
      'id -> itemRecord.bibIds.head) match {
      case Some(Right(mergedRecord)) =>
        val newRecord = mergedRecord.mergeItemRecord(itemRecord)
        if (newRecord != mergedRecord)
          Scanamo.put[MergedSierraRecord](dynamoDBClient)(dynamoConfig.table)(
            newRecord.copy(version = mergedRecord.version + 1))
    }
//    logger.info(s"Attempting to update $itemRecord")
//
//    val existingRecord = Scanamo.exec(dynamoDBClient)(
//      table.get('id -> itemRecord.)
//    )
//
//    val newRecord = existingRecord
//      .map {
//        case Left(error) =>
//          Left(
//            new RuntimeException(error.toString)
//          )
//        case Right(record) => {
//          logger.info(s"Found $record, attempting merge.")
//
//          record
//            .mergeItemRecord(itemRecord)
//            .toRight(
//              new RuntimeException("Unable to merge record!")
//            )
//        }
//      }
//      .getOrElse {
//        val record = MergedSierraRecord(itemRecord)
//        logger.info(s"No match found, creating new record: $record")
//
//        Right(record)
//      }
//
//    val putOperation = newRecord match {
//      case Right(record) => {
//        logger.info(s"Attempting to conditionally update $record.")
//
//        Scanamo
//          .exec(dynamoDBClient)(
//            putRecord(record)
//          )
//          .left
//          .map(e => new RuntimeException(e.toString))
//      }
//      case Left(e) => Left(e)
//    }
//
//    putOperation match {
//      case Right(_) =>
//        logger.info(s"${itemRecord.id} saved successfully to DynamoDB")
//      case Left(error) =>
//        logger.warn(s"Failed processing ${itemRecord.id}", error)
//    }
  }
}
