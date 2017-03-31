package uk.ac.wellcome.platform.transformer.actors

import javax.inject.Inject

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws._
import uk.ac.wellcome.platform.transformer.modules.{
  KinesisWorker,
  ActorRegistryModule
}

import uk.ac.wellcome.models.ActorRegister

import com.amazonaws.services.sns.AmazonSNS

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Success
import scala.util.Failure
import com.google.inject.name.Named

@Named("PublishableMessageRecordActor")
class PublishableMessageRecordActor @Inject()(
  actorRegister: ActorRegister,
  snsConfig: SNSConfig,
  snsClient: AmazonSNS
) extends Actor
    with Logging {

  def receive = {
    case unifiedItem: UnifiedItem => {
      JsonUtil.toJson(unifiedItem) match {
        case Success(stringifiedJson) => {
          val message = SNSMessage(
            Some("Foo"),
            stringifiedJson,
            snsConfig.topicArn,
            snsClient
          )

          info(s"Publishable message ${message}")
          actorRegister.send("publisherActor", message)
        }
        case Failure(e) => {
          // TODO: Send to dead letter queue or just error
          error("Failed to convert into publishable message", e)
        }
      }
    }
    case record => error(s"Received unidentified record ${record}")
  }
}
