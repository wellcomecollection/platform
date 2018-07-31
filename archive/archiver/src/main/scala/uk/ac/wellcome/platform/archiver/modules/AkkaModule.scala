package uk.ac.wellcome.platform.archiver.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Injector, Provides, Singleton}

object AkkaModule extends AbstractModule {
  @Singleton
  @Provides
  def providesActorSystem(injector: Injector): ActorSystem = {
    val system = ActorSystem("main-actor-system")

    GuiceAkkaExtension(system).initialize(injector)

    system
  }
}
