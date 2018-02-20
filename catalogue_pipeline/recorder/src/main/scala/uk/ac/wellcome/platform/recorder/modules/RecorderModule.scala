package uk.ac.wellcome.platform.recorder.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.utils.TryBackoff

object SierraBibMergerModule extends TwitterModule with TryBackoff {

  override lazy val continuous: Boolean = false

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[RecorderWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[RecorderWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
