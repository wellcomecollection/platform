package uk.ac.wellcome.platform.transformer.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.platform.transformer.services.TransformerWorkerService

object TransformerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[TransformerWorkerService[_ <: Transformable]]

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService =
      injector.instance[TransformerWorkerService[_ <: Transformable]]

    workerService.stop()
    system.terminate()
  }
}
