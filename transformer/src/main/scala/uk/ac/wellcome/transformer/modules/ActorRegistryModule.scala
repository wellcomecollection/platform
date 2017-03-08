package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import akka.actor.{ActorRef, ActorSystem, Props}
import uk.ac.wellcome.platform.transformer.actors._

import uk.ac.wellcome.utils.GuiceAkkaExtension
import net.codingwell.scalaguice.ScalaModule

import akka.actor.Actor
import com.google.inject.name.Names

case class ActorRegister(actors: Map[String, ActorRef])

object ActorRegistryModule
  extends TwitterModule {

  override val modules = Seq(
    DynamoConfigModule,
    AkkaModule,
    WorkerConfigModule,
    SNSClientModule
  )

  override def configure() {
    bind[Actor]
      .annotatedWith(Names.named("KinesisDynamoRecordExtractorActor"))
      .to[KinesisDynamoRecordExtractorActor]

    bind[Actor]
      .annotatedWith(Names.named("DynamoCaseClassExtractorActor"))
      .to[DynamoCaseClassExtractorActor]

    bind[Actor]
      .annotatedWith(Names.named("TransformActor"))
      .to[TransformActor]

    bind[Actor]
      .annotatedWith(Names.named("PublishableMessageRecordActor"))
      .to[PublishableMessageRecordActor]

    bind[Actor]
      .annotatedWith(Names.named("PublisherActor"))
      .to[PublisherActor]
  }

  @Singleton
  @Provides
  def provideActorRegistry(system: ActorSystem): ActorRegister  = {
     ActorRegister(Map(

      "kinesisDynamoRecordExtractorActor" ->
        system.actorOf(GuiceAkkaExtension(system).props(
          "KinesisDynamoRecordExtractorActor")),

      "dynamoCaseClassExtractorActor" ->
        system.actorOf(GuiceAkkaExtension(system).props(
          "DynamoCaseClassExtractorActor")),

      "transformActor" ->
        system.actorOf(GuiceAkkaExtension(system).props(
          "TransformActor")),

      "publishableMessageRecordActor" ->
        system.actorOf(GuiceAkkaExtension(system).props(
          "PublishableMessageRecordActor")),

      "publisherActor" ->
        system.actorOf(GuiceAkkaExtension(system).props(
          "PublisherActor"))
    ))
  }
}


