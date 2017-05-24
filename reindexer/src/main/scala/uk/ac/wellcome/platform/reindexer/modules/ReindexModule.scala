package uk.ac.wellcome.platform.reindexer.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindexer.services.ReindexService
import uk.ac.wellcome.utils.TryBackoff

object ReindexModule extends TwitterModule  with TryBackoff {

  override def singletonStartup(injector: Injector) {
    info("Starting Reindexer module")

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = injector.instance[ReindexService]

    run(() => reindexService.run, actorSystem)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")
    cancelRun()
    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
