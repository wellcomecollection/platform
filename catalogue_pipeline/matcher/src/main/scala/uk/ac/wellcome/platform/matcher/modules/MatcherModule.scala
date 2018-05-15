package uk.ac.wellcome.platform.matcher.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.matcher.MatcherMessageReceiver

object MatcherModule extends TwitterModule {
  override def singletonStartup(injector: Injector): Unit = {
    super.singletonStartup(injector)
    injector.instance[MatcherMessageReceiver]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Matcher worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[MatcherMessageReceiver]

    workerService.stop()
    system.terminate()
  }
}
