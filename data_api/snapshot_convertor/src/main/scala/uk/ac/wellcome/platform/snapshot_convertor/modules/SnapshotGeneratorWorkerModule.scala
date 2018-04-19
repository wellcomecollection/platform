package uk.ac.wellcome.platform.snapshot_convertor.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.snapshot_convertor.services.SnapshotGeneratorWorkerService

object SnapshotGeneratorWorkerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SnapshotGeneratorWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SnapshotGeneratorWorkerService]

    try {
      workerService.stop()
    } finally {
      system.terminate()
    }
  }

}
