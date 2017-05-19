package uk.ac.wellcome.platform.calm_adapter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import akka.actor.{ActorRef, ActorSystem, Props}
import uk.ac.wellcome.platform.calm_adapter.actors._
import uk.ac.wellcome.platform.calm_adapter.services._
import uk.ac.wellcome.utils.GuiceAkkaExtension
import net.codingwell.scalaguice.ScalaModule
import akka.actor.Actor
import com.google.inject.name.Names
import uk.ac.wellcome.calm_adapter.models.ActorRegister
import uk.ac.wellcome.finatra.modules.{AkkaModule, DynamoClientModule, DynamoConfigModule}

object ActorRegistryModule extends TwitterModule {

  override val modules = Seq(
    DynamoConfigModule,
    DynamoClientModule,
    AkkaModule
  )

  override def configure() {
    bind[Actor]
      .annotatedWith(Names.named("OaiParserActor"))
      .to[OaiParserActor]

    bind[Actor]
      .annotatedWith(Names.named("OaiHarvestActor"))
      .to[OaiHarvestActor]

    bind[Actor]
      .annotatedWith(Names.named("DynamoRecordWriterActor"))
      .to[DynamoRecordWriterActor]
  }

  @Singleton
  @Provides
  def provideActorRegistry(system: ActorSystem): ActorRegister = {
    ActorRegister(
      Map(
        "oaiParserActor" ->
          system.actorOf(GuiceAkkaExtension(system).props("OaiParserActor")),
        "oaiHarvestActor" ->
          system.actorOf(GuiceAkkaExtension(system).props("OaiHarvestActor")),
        "dynamoRecordWriterActor" ->
          system.actorOf(
            GuiceAkkaExtension(system).props("DynamoRecordWriterActor"))
      ))
  }
}
