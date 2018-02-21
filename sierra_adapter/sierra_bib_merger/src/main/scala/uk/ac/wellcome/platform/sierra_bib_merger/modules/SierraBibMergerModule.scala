package uk.ac.wellcome.platform.sierra_bib_merger.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_bib_merger.services.{
  SierraBibMergerUpdaterService,
  SierraBibMergerWorkerService
}
import uk.ac.wellcome.utils.TryBackoff

object SierraBibMergerModule extends TwitterModule with TryBackoff {

  override lazy val continuous: Boolean = false

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
