package uk.ac.wellcome.platform.matcher.modules

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

import uk.ac.wellcome.platform.matcher.GlobalExecutionContext

import scala.concurrent.ExecutionContext
import uk.ac.wellcome.platform.matcher.services.MatcherWorkerService

object MatcherModule extends TwitterModule {
  override def singletonStartup(injector: Injector): Unit = {
    super.singletonStartup(injector)
    injector.instance[MatcherWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Matcher worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[MatcherWorkerService]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def provideRecorderExecutionContext(): ExecutionContext =
    GlobalExecutionContext.context
}
