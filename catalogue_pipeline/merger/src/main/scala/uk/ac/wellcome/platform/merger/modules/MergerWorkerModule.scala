package uk.ac.wellcome.platform.merger.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.transformer.services.MergerWorkerService

object MergerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    injector.instance[MergerWorkerService]

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[MergerWorkerService]

    workerService.stop()
    system.terminate()
  }
}
