package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.google.inject.Inject
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging
import uk.ac.wellcome.models.{CalmDynamoRecord, Transformable, UnifiedItem}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class RecordMap(value: java.util.Map[String, AttributeValue])

class RecordReceiver @Inject()(snsWriter: SNSWriter) extends Logging {

  def receiveRecord(record: RecordAdapter): Future[PublishAttempt] = {
    info(s"Starting to process record $record")

    val triedUnifiedItem = for {
      recordMap <- recordToRecordMap(record)
      transformableRecord <- extractTransformableCaseClass(recordMap)
      cleanRecord <- transformDynamoRecord(transformableRecord)
    } yield cleanRecord

    triedUnifiedItem match {
      case Success(unifiedItem) => publishMessage(unifiedItem)
      case Failure(e) =>
        error("Failed extracting unified item from record", e)
        Future.failed(e)
    }
  }

  def recordToRecordMap(record: RecordAdapter): Try[RecordMap] = Try {
    val keys = record.getInternalObject.getDynamodb.getNewImage

    info(s"Received record $keys")
    RecordMap(keys)
  }

  def extractTransformableCaseClass(record: RecordMap): Try[Transformable] = {
    Try { ScanamoFree.read[CalmDynamoRecord](record.value) }.map {
      case Right(calmDynamoRecord) =>
        info(s"Parsed DynamoDB record $calmDynamoRecord")
        calmDynamoRecord
      case Left(dynamoReadError) =>
        error(s"Unable to parse record ${record.value}")
        throw new Exception(
          s"Unable to parse record ${record.value} received $dynamoReadError")
    }.recover {
      case e: Throwable =>
        error("Error extracting transformable case class", e)
        throw e
    }
  }

  def transformDynamoRecord(dirtyRecord: Transformable): Try[UnifiedItem] = {
    dirtyRecord.transform map {cleanRecord =>
        info(s"Cleaned record $cleanRecord")
        cleanRecord
    } recover {
      case e: Throwable =>
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to clean record", e)
        throw e
    }
  }

  def publishMessage(unifiedItem: UnifiedItem): Future[PublishAttempt] =
    snsWriter.writeMessage(JsonUtil.toJson(unifiedItem).get, Some("Foo"))
}
