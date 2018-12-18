package uk.ac.wellcome.platform.api.akka

import akka.actor.ActorSystem
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule

import scala.concurrent.ExecutionContext

object ExecutionContextModule extends TwitterModule {
  override val modules = Seq(AkkaModule)

  @Provides
  @Singleton
  def provideExecutionContext(actorSystem: ActorSystem): ExecutionContext =
    actorSystem.dispatcher
}
