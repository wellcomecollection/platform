package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.transformer.modules.PublishableMessage

import scala.util.Success
import scala.util.Failure
import com.google.inject.name.Named


@Named("PublisherActor")
class PublisherActor()
  extends Actor
  with Logging {

  def receive = {
    case m: PublishableMessage => m.publish match {
      case Success(publishAttempt) => {
        info(s"Published message ${publishAttempt.id}")
      }
      case Failure(e) => {
        error("Failed to publish message", e)
      }
    }
    case o => error(s"Received non-message ${o}")
  }
}
