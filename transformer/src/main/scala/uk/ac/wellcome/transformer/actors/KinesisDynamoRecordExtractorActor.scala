package uk.ac.wellcome.platform.transformer.actors

import javax.inject.Inject

import akka.actor.Actor
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.name.Named
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.{SNSConfig, SNSMessage}
import uk.ac.wellcome.models.{ActorRegister, CalmDynamoRecord, Transformable, UnifiedItem}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class RecordMap(value: java.util.Map[String, AttributeValue])

@Named("KinesisDynamoRecordExtractorActor")
class KinesisDynamoRecordExtractorActor @Inject()(
  actorRegister: ActorRegister,
  snsConfig: SNSConfig,
  snsClient: AmazonSNS
) extends Actor
    with Logging {

  def prepareRecordForPublishing(unifiedItem: UnifiedItem): Future[SNSMessage] = {
    Future.fromTry(JsonUtil.toJson(unifiedItem)).transform(stringifiedJson => {
      val message = SNSMessage(
        Some("Foo"),
        stringifiedJson,
        snsConfig.topicArn,
        snsClient
      )
      info(s"Publishable message $message")
      message
    },
      e => {
        error("Failed to convert into publishable message", e)
        e
      })
  }

  def publishMessage(message: SNSMessage): Future[Unit] = Future{
    message.publish() match {
      case Success(publishAttempt) =>
        info(s"Published message ${publishAttempt.id}")
      case Failure(e) =>
        error("Failed to publish message", e)
    }
  }

  def transformDynamoRecord(dirtyRecord: Transformable): Future[UnifiedItem] = {
    Future.fromTry(dirtyRecord.transform).transform(
      cleanRecord => {
        info(s"Cleaned record $cleanRecord")
        cleanRecord
      },
      e => {
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to clean record", e)
        e
      })
  }

  def extractDynamoCaseClass(record: RecordMap): Future[Transformable] = {
    Future {ScanamoFree.read[CalmDynamoRecord](record.value)}.map {
      case Right(calmDynamoRecord) =>
        info(s"Parsed DynamoDB record $calmDynamoRecord")
        calmDynamoRecord
      case Left(dynamoReadError) =>
        error(s"Unable to parse record ${record.value}")
        throw new Exception(s"Unable to parse record ${record.value} received $dynamoReadError")
    }
  }



  def receive = {
    case record: RecordAdapter => {
      val keys = record
        .getInternalObject()
        .getDynamodb()
        .getNewImage()

      info(s"Received record ${keys}")

      for {
        o <- extractDynamoCaseClass(RecordMap(keys))
        cleanRecord <- transformDynamoRecord(o)
        snsMessage  <- prepareRecordForPublishing(cleanRecord)
        _           <- publishMessage(snsMessage)
      } yield ()

      actorRegister.send("dynamoCaseClassExtractorActor", RecordMap(keys))
    }
    case event => error(s"Received unknown Kinesis event ${event}")
  }
}
