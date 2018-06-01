package uk.ac.wellcome.platform.sierra_reader.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService

object SierraReaderModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraReaderWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Sierra Bibs to SNS worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
