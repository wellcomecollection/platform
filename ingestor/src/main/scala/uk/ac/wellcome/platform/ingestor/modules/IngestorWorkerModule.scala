package uk.ac.wellcome.platform.ingestor.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.ingestor.services.IdMinterWorkerService

object IngestorWorkerModule extends TwitterModule {
  private val esIndex = flag[String]("es.index", "records", "ES index name")
  private val esType = flag[String]("es.type", "item", "ES document type")

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[IdMinterWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[IdMinterWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
