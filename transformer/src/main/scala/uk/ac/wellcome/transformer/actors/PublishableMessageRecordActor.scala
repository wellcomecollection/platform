package uk.ac.wellcome.platform.transformer.actors

import javax.inject.Inject

import akka.actor.Actor
import com.twitter.inject.Logging
import com.twitter.inject.TwitterModule

import uk.ac.wellcome.platform.transformer.models._
import uk.ac.wellcome.platform.transformer.modules.{
  SNSMessage,
  KinesisWorker,
  WorkerConfig
}

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Success
import scala.util.Failure


class PublishableMessageRecordActor @Inject()(workerConfig: WorkerConfig)
  extends TwitterModule
  with Actor
  with Logging {

  def receive = {
    case cleanedRecord: CleanedRecord => {
      JsonUtil.toJson(cleanedRecord) match {
        case Success(stringifiedJson) => {
          val message = SNSMessage(
            cleanedRecord.source,
            stringifiedJson,
            workerConfig.snsTopicArn
          )

          info(s"Publishable message ${message}")

          KinesisWorker.publisherActor ! message
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
