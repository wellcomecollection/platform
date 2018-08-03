package uk.ac.wellcome.platform.merger.modules

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.platform.merger.services.{Merger, MergerRules, MergerWorkerService}

object MergerWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    injector.instance[MergerWorkerService]

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[MergerWorkerService]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def providesMergerRules(): MergerRules =
    new Merger()
}
