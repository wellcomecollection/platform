package uk.ac.wellcome.platform.transformer.sierra.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.transformer.sierra.services.SierraTransformerWorkerService

object SierraTransformerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    injector.instance[SierraTransformerWorkerService]

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraTransformerWorkerService]

    workerService.stop()
    system.terminate()
  }
}
