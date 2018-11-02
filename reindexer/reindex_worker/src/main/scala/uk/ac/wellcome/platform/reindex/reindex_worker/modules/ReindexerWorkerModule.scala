package uk.ac.wellcome.platform.reindex.reindex_worker.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex.reindex_worker.services.ReindexWorker

object ReindexerWorkerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[ReindexWorker]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexWorker]

    workerService.stop()
    system.terminate()
  }

}
