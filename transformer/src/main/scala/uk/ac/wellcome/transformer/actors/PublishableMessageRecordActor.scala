package uk.ac.wellcome.platform.transformer.actors

import javax.inject.Inject

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.transformer.models._
import uk.ac.wellcome.platform.transformer.modules.{
  SNSMessage,
  KinesisWorker,
  WorkerConfig,
  ActorRegistryModule
}

import uk.ac.wellcome.platform.transformer.modules.ActorRegister

import com.amazonaws.services.sns.AmazonSNS

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Success
import scala.util.Failure
import com.google.inject.name.Named

@Named("PublishableMessageRecordActor")
class PublishableMessageRecordActor @Inject()(
  actorRegister: ActorRegister,
  workerConfig: WorkerConfig,
  snsClient: AmazonSNS
) extends Actor
    with Logging {

  def receive = {
    case cleanedRecord: CleanedRecord => {
      JsonUtil.toJson(cleanedRecord) match {
        case Success(stringifiedJson) => {
          val message = SNSMessage(
            cleanedRecord.source,
            stringifiedJson,
            workerConfig.snsTopicArn,
            snsClient
          )

          info(s"Publishable message ${message}")

          actorRegister.actors
            .get("publisherActor")
            .map(_ ! message)
        }
        case Failure(e) => {
          // Send to dead letter queue or just error
          error("Failed to convert into publishable message", e)
        }
      }
    }
    case record => error(s"Received unidentified record ${record}")
  }
}
