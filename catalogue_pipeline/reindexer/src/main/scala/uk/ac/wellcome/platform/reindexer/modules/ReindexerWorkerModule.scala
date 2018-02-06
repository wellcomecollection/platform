package uk.ac.wellcome.platform.reindexer.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindexer.services.ReindexerWorkerService

object ReindexerWorkerModule extends TwitterModule {
  flag[String]("reindex.sourceData.tableName", "SourceData", "Name of the DynamoDB SourceData table")

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
