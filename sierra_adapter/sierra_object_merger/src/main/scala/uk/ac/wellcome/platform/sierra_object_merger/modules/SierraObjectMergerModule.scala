package uk.ac.wellcome.platform.sierra_object_merger.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_object_merger.services.SierraObjectMergerWorkerService

object SierraObjectMergerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraToDynamoWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraObjectMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
