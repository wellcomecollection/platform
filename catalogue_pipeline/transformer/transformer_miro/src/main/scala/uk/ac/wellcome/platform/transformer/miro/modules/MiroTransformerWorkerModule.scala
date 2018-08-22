package uk.ac.wellcome.platform.transformer.miro.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.transformer.miro.services.MiroTransformerWorkerService

object MiroTransformerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    injector.instance[MiroTransformerWorkerService]

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[MiroTransformerWorkerService]

    workerService.stop()
    system.terminate()
  }
}
