package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.transformer.models._
import scala.util.Success
import scala.util.Failure

import uk.ac.wellcome.platform.transformer.modules._


class TransformActor extends Actor with Logging {
  def receive = {
    case dirtyRecord: Transformable => {
      dirtyRecord.transform match {
        case Success(cleanRecord) => {
          info(s"Cleaned record ${cleanRecord}")
          KinesisWorker.publishableMessageRecordActor ! cleanRecord
        }
        case Failure(e) => {
          // Send to dead letter queue or just error
          error("Failed to perform transform to clean record", e)
        }
      }
    }
    case record => error(s"Received non-transformable record ${record}")
  }
}
