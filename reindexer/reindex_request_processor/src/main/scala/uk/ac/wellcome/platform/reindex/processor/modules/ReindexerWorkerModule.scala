package uk.ac.wellcome.platform.reindex.processor.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex.processor.services.ReindexWorkerService

object ReindexerWorkerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[ReindexWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexWorkerService]

    workerService.stop()
    system.terminate()
  }
}
