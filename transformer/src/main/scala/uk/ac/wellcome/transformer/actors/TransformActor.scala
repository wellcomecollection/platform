package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.twitter.inject.Logging
import uk.ac.wellcome.models._

import scala.util.Success
import scala.util.Failure
import javax.inject.Inject

import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.platform.transformer.modules.ActorRegistryModule
import uk.ac.wellcome.models.ActorRegister
import com.google.inject.name.Named
import org.scalacheck.Prop.Exception
import uk.ac.wellcome.models.aws.{SNSConfig, SNSMessage}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

@Named("TransformActor")
class TransformActor @Inject()(
  actorRegister: ActorRegister,
  snsConfig: SNSConfig,
  snsClient: AmazonSNS
) extends Actor
    with Logging {

  def prepareRecordForPublishing(unifiedItem: UnifiedItem): Future[SNSMessage] = {
    JsonUtil.toJson(unifiedItem) match {
      case Success(stringifiedJson) => Future {
        val message = SNSMessage(
          Some("Foo"),
          stringifiedJson,
          snsConfig.topicArn,
          snsClient
        )
        info(s"Publishable message ${message}")
        message
      }
      case Failure(e) => {
        error("Failed to convert into publishable message", e)
        Future.failed(e)
      }
    }
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

  def receive = {
    case dirtyRecord: Transformable => {
      dirtyRecord.transform match {
        case Success(cleanRecord) => {
          info(s"Cleaned record ${cleanRecord}")
          prepareRecordForPublishing(cleanRecord).map(publishMessage)

        }
        case Failure(e) => {
          // TODO: Send to dead letter queue or just error
          error("Failed to perform transform to clean record", e)
        }
      }
    }
    case record => error(s"Received non-transformable record ${record}")
  }
}
