package uk.ac.wellcome.platform.recorder.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService

object RecorderModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[RecorderWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[RecorderWorkerService]

    workerService.stop()
    system.terminate()
  }
}
