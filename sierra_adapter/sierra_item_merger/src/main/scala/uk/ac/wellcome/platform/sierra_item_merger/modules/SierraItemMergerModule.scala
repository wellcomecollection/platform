package uk.ac.wellcome.platform.sierra_item_merger.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_item_merger.services.SierraItemMergerWorkerService
import uk.ac.wellcome.utils.TryBackoff

object SierraItemMergerModule extends TwitterModule with TryBackoff {

  override lazy val continuous: Boolean = false

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraItemMergerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraItemMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
