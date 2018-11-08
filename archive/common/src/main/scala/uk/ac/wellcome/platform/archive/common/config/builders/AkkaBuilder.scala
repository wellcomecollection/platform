package uk.ac.wellcome.platform.archive.common.config.builders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object AkkaBuilder {
  def buildActorSystem(): ActorSystem =
    ActorSystem("main-actor-system")

  def buildActorMaterializer(): ActorMaterializer = {
    implicit val actorSystem = buildActorSystem()
    ActorMaterializer()
  }
}
