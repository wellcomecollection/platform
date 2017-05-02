package uk.ac.wellcome.calm_adapter.models

import akka.actor.ActorRef

case class ActorRegister(actors: Map[String, ActorRef]) {
  def send[T](
    actorName: String,
    message: T
  ): Option[Unit] =
    actors
      .get(actorName)
      .map(_ ! message)
}
