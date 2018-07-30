package uk.ac.wellcome.messaging.fixtures

import akka.actor.ActorSystem
import org.scalatest.concurrent.Eventually

trait Akka extends Eventually {
  private[messaging] def withMessagingActorSystem[R] = fixture[ActorSystem, R](
    create = ActorSystem(),
    destroy = eventually { _.terminate() }
  )
}
