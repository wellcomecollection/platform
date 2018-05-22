package uk.ac.wellcome.platform.sierra_item_merger.modules

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.platform.sierra_item_merger.GlobalExecutionContext
import uk.ac.wellcome.platform.sierra_item_merger.services.SierraItemMergerWorkerService

import scala.concurrent.ExecutionContext

object SierraItemMergerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraItemMergerWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val workerService = injector.instance[SierraItemMergerWorkerService]
    val system = injector.instance[ActorSystem]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def provideRecorderExecutionContext(): ExecutionContext =
    GlobalExecutionContext.context
}
