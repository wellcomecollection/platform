package uk.ac.wellcome.platform.archive.common.config.builders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

object AkkaBuilder {
  implicit val actorSystem: ActorSystem = ActorSystem("main-actor-system")

  def buildActorSystem(): ActorSystem =
    actorSystem

  def buildActorMaterializer(): ActorMaterializer =
    ActorMaterializer()

  def buildExecutionContext(): ExecutionContext =
    actorSystem.dispatcher
}
