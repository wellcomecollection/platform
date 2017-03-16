package uk.ac.wellcome.platform.calm_adapter.actors

import akka.actor._
import com.twitter.inject.Logging
import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.platform.calm_adapter.modules.ActorRegister


case class SlowDown(m: DeadLetter)

@Named("PipelineWatcherActor")
class PipelineWatcherActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  def receive = {
    case d: DeadLetter => {
     info(s"Watcher recieved dead Letter: ${d}")

     actorRegister.actors
       .get("OaiHarvestActor")
       .map(_ ! SlowDown(d))
    }
    case m => error(s"Received unknown message ${m}")
  }
}
