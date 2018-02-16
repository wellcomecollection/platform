package uk.ac.wellcome.platform.reindex_worker.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex_worker.services.ReindexerWorkerService

object ReindexerWorkerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[ReindexerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
