package uk.ac.wellcome.platform.reindex_worker.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex_worker.services.ReindexWorkerService

object ReindexerWorkerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[ReindexWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
