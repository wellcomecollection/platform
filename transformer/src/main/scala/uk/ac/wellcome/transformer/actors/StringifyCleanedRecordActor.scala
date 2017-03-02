package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.transformer.models._

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Success
import scala.util.Failure


class StringifyCleanedRecordActor extends Actor with Logging {
  def receive = {
    case cleanedRecord: CleanedRecord => {
      JsonUtil.toJson(cleanedRecord) match {
        case Success(s) => {
          info(s"Stringified record as json ${s}")
          // Send to next actor
        }
        case Failure(e) => {
          // Send to dead letter queue or just error
          error("Failed to perform json serialization", e)
        }
      }
    }
    case record => error(s"Received unidentified record ${record}")
  }
}
