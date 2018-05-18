package uk.ac.wellcome.finatra.akka

import javax.inject.Singleton
import akka.actor.ActorSystem
import com.google.inject.Provides
import com.google.inject.Injector
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
