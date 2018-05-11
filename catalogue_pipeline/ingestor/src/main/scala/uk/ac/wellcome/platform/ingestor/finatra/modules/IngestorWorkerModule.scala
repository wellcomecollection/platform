package uk.ac.wellcome.platform.ingestor.finatra.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.ingestor.services.IngestorWorkerService

object IngestorWorkerModule extends TwitterModule {
  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[IngestorWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[IngestorWorkerService]

    workerService.stop()
    system.terminate()
  }
}
