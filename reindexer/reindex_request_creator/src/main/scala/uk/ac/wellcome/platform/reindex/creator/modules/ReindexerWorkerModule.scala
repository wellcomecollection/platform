package uk.ac.wellcome.platform.reindex.creator.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindex.creator.services.ReindexRequestCreatorWorker

object ReindexerWorkerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[ReindexRequestCreatorWorker]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[ReindexRequestCreatorWorker]

    workerService.stop()
    system.terminate()
  }

}
