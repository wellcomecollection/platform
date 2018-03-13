package uk.ac.wellcome.platform.snapshot_convertor.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.snapshot_convertor.services.SnapshotConvertorWorkerService

object SnapshotConvertorWorkerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SnapshotConvertorWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SnapshotConvertorWorkerService]

    workerService.stop()
    system.terminate()
  }

}
