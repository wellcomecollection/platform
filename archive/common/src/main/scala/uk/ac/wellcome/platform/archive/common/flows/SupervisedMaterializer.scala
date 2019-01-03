package uk.ac.wellcome.platform.archive.common.flows

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import grizzled.slf4j.Logging

object SupervisedMaterializer extends Logging {
  private def resumeDecider: Supervision.Decider = { e =>
    error("Stream failure", e)
    Supervision.Resume
  }

  def resumable(implicit actorSystem: ActorSystem) = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(
      resumeDecider)
  )
}