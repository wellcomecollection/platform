package uk.ac.wellcome.platform.goobi_reader.modules

import akka.actor.ActorSystem
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.goobi_reader.services.GoobiReaderWorkerService

import scala.concurrent.ExecutionContext

object GoobiReaderModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[GoobiReaderWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    val system = injector.instance[ActorSystem]
    system.terminate()
  }

  @Singleton
  @Provides
  def providesExecutionContext(system: ActorSystem): ExecutionContext =
    system.dispatcher
}
