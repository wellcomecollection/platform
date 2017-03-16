package uk.ac.wellcome.platform.calm_adapter.actors

import akka.actor._
import com.twitter.inject.Logging
import com.google.inject.name.Named
import javax.inject.Inject


@Named("PipelineWatcherActor")
class PipelineWatcherActor  @Inject()()
  extends Actor
  with Logging {

  def receive = {
    case d: DeadLetter => {
     info(s"Dead Letter: ${d}")
    }
    case unknown => error(s"Received unknown object ${unknown}")
  }
}
