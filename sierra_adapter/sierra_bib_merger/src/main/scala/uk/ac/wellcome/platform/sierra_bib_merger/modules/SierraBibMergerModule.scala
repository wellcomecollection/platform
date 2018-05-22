package uk.ac.wellcome.platform.sierra_bib_merger.modules

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.platform.sierra_bib_merger.GlobalExecutionContext
import uk.ac.wellcome.platform.sierra_bib_merger.services.SierraBibMergerWorkerService

import scala.concurrent.ExecutionContext

object SierraBibMergerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraBibMergerWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def provideRecorderExecutionContext(): ExecutionContext =
    GlobalExecutionContext.context

}
