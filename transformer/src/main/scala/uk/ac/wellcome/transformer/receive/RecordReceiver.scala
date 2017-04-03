package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.{SNSConfig, SNSMessage}
import uk.ac.wellcome.models.{CalmDynamoRecord, Transformable, UnifiedItem}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

case class RecordMap(value: java.util.Map[String, AttributeValue])

class RecordReceiver @Inject()(snsConfig: SNSConfig,
                               snsClient: AmazonSNS) extends Logging {

  def receiveRecord(record: RecordAdapter): Future[Unit] = {
    for {
      recordMap           <- recordToRecordMap(record)
      transformableRecord <- extractTransformableCaseClass(recordMap)
      cleanRecord         <- transformDynamoRecord(transformableRecord)
      snsMessage          <- buildSNSMessageFrom(cleanRecord)
      _                   <- publishMessage(snsMessage)
    } yield ()
  }

  def recordToRecordMap(record: RecordAdapter): Future[RecordMap] = Future {
    val keys = record
      .getInternalObject()
      .getDynamodb()
      .getNewImage()

    info(s"Received record ${keys}")
    RecordMap(keys)
  }

  def extractTransformableCaseClass(record: RecordMap): Future[Transformable] = {
    Future {ScanamoFree.read[CalmDynamoRecord](record.value)}.map {
      case Right(calmDynamoRecord) =>
        info(s"Parsed DynamoDB record $calmDynamoRecord")
        calmDynamoRecord
      case Left(dynamoReadError) =>
        error(s"Unable to parse record ${record.value}")
        throw new Exception(s"Unable to parse record ${record.value} received $dynamoReadError")
    }
  }

  def transformDynamoRecord(dirtyRecord: Transformable): Future[UnifiedItem] = {
    Future{dirtyRecord.transform}.map {
      case Success(cleanRecord) =>
        info(s"Cleaned record $cleanRecord")
        cleanRecord
      case Failure(e) =>
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to clean record", e)
        throw e
    }
  }

  def buildSNSMessageFrom(unifiedItem: UnifiedItem): Future[SNSMessage] = {
    Future{JsonUtil.toJson(unifiedItem)}.map {
      case Success(stringifiedJson) =>
        val message = SNSMessage(
          Some("Foo"),
          stringifiedJson,
          snsConfig.topicArn,
          snsClient
        )
        info(s"Publishable message $message")
        message
      case Failure(e) =>
        error("Failed to convert into publishable message", e)
        throw e
    }
  }

  def publishMessage(message: SNSMessage): Future[Unit] = Future{
    message.publish() match {
      case Success(publishAttempt) =>
        info(s"Published message ${publishAttempt.id}")
      case Failure(e) =>
        error("Failed to publish message", e)
        throw e
    }
  }
}