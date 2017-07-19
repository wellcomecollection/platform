package uk.ac.wellcome.transformer.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.transformer.services.TransformerWorkerService

object TransformerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[TransformerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[TransformerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
