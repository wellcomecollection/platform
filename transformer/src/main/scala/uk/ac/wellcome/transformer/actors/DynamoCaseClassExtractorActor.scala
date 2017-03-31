package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.transformer.modules._
import uk.ac.wellcome.models._
import javax.inject.Inject

import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.platform.transformer.modules.ActorRegistryModule
import com.google.inject.name.Named
import uk.ac.wellcome.models.aws.{SNSConfig, SNSMessage}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Named("DynamoCaseClassExtractorActor")
class DynamoCaseClassExtractorActor @Inject()(
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
        info(s"Publishable message ${message}")
        message
      },
      e => {
        error("Failed to convert into publishable message", e)
        e
      })
  }

  def publishMessage(message: SNSMessage): Future[Unit] = Future{
    message.publish match {
      case Success(publishAttempt) => {
        info(s"Published message ${publishAttempt.id}")
      }
      case Failure(e) => {
        error("Failed to publish message", e)
      }
    }
  }

  def transformDynamoRecord(dirtyRecord: Transformable): Future[UnifiedItem] = {
    Future.fromTry(dirtyRecord.transform).transform(
      cleanRecord => {
        info(s"Cleaned record ${cleanRecord}")
        cleanRecord
      },
      e => {
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to clean record", e)
        e
      })
    }

  def receive = {
    case record: RecordMap => {
      ScanamoFree.read[CalmDynamoRecord](record.value) match {
        case Right(o) => {
          info(s"Parsed DynamoDB record ${o}")
          for {
            cleanRecord <- transformDynamoRecord(o)
            snsMessage  <- prepareRecordForPublishing(cleanRecord)
            _           <- publishMessage(snsMessage)
          } yield ()
        }
        case Left(o) => {
          error(s"Unable to parse record ${o}")
          // TODO: Send to dead letter queue or error
        }
      }
    }
    case event => error(s"Received unknown DynamoDB record ${event}")
  }
}
