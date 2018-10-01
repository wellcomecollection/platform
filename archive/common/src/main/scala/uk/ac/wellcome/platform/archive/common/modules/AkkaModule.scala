package uk.ac.wellcome.platform.archive.common.modules

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Injector, Provides, Singleton}

import scala.concurrent.ExecutionContext

object AkkaModule extends AbstractModule {
  @Singleton
  @Provides
  def providesActorSystem(injector: Injector): ActorSystem = {
    val system = ActorSystem("main-actor-system")

    GuiceAkkaExtension(system).initialize(injector)

    system
  }

  @Singleton
  @Provides
  def providesMaterializer(implicit system: ActorSystem): ActorMaterializer =
    ActorMaterializer()

  @Singleton
  @Provides
  def providesExecutionContext(actorSystem: ActorSystem) = {
    actorSystem.dispatcher.asInstanceOf[ExecutionContext]
  }
}
