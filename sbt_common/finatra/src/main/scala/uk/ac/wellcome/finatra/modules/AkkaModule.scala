package uk.ac.wellcome.finatra.modules

import akka.actor.ActorSystem
import com.google.inject.{Injector, Provides}
import com.twitter.inject.{InjectorModule, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.finatra.utils.GuiceAkkaExtension

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
