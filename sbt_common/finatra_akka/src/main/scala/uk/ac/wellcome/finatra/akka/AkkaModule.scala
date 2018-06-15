package uk.ac.wellcome.finatra.akka

import akka.actor.ActorSystem
import com.google.inject.{Injector, Provides, Singleton}
import com.twitter.inject.{InjectorModule, TwitterModule}

object AkkaModule extends TwitterModule {
  override val modules = Seq(InjectorModule)

  @Singleton
  @Provides
  def providesActorSystem(injector: Injector): ActorSystem = {
    val system = ActorSystem("main-actor-system")
    GuiceAkkaExtension(system).initialize(injector)
    system
  }
}
