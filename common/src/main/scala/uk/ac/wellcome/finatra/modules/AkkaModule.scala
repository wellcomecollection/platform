package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton
import uk.ac.wellcome.utils.GuiceAkkaExtension
import akka.actor.ActorSystem
import com.google.inject.Provides
import com.google.inject.Injector
import com.twitter.inject.{TwitterModule, InjectorModule}

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
