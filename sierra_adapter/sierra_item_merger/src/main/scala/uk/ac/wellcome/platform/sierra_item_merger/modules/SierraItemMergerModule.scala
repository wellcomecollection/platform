package uk.ac.wellcome.platform.sierra_item_merger.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_item_merger.services.SierraItemMergerWorkerService

object SierraItemMergerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraItemMergerWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val workerService = injector.instance[SierraItemMergerWorkerService]
    val system = injector.instance[ActorSystem]

    workerService.stop()
    system.terminate()
  }

}
