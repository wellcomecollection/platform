package uk.ac.wellcome.platform.reindex.processor.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex.processor.services.ReindexRequestProcessorWorker

object ReindexerWorkerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[ReindexRequestProcessorWorker]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexRequestProcessorWorker]

    workerService.stop()
    system.terminate()
  }
}
