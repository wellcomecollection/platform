package uk.ac.wellcome.platform.calm_adapter.modules

import java.nio.charset.{Charset => JCharset}
import java.util.{List => JList}
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props, DeadLetter}
import com.amazonaws.auth.{
  AWSCredentials,
  AWSCredentialsProvider,
  DefaultAWSCredentialsProviderChain
}
import com.amazonaws.regions._
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.sns._
import com.twitter.inject.{Injector, TwitterModule}

import uk.ac.wellcome.models.ActorRegister

import uk.ac.wellcome.platform.calm_adapter.actors._
import uk.ac.wellcome.platform.finatra.modules._
import uk.ac.wellcome.platform.finatra.modules.AkkaModule
import uk.ac.wellcome.utils._

object CalmAdapterWorker extends TwitterModule {

  override val modules =
    Seq(ActorRegistryModule, AkkaModule, SNSClientModule, SNSConfigModule)

  val warmupTime =
    flag(
      name = "warmupTime",
      default = 1,
      help = "Seconds to wait for dynamo scaling to occur, before starting."
    )

  override def singletonStartup(injector: Injector) {
    info("Starting Adapter worker")

    val system = injector.instance[ActorSystem]
    val actorRegister = injector.instance[ActorRegister]

    actorRegister.actors
      .get("pipelineWatcherActor")
      .map(actorRef => {
        system.eventStream
          .subscribe(actorRef, classOf[DeadLetter])
      })

    system.scheduler.scheduleOnce(
      Duration.create(warmupTime(), TimeUnit.SECONDS)
    )(
      actorRegister.send("oaiHarvestActor", oaiHarvestActorConfig)
    )
  }

  val oaiHarvestActorConfig =
    OaiHarvestActorConfig(
      verb = "ListRecords",
      metadataPrefix = Some("calm_xml")
    )

  override def singletonShutdown(injector: Injector) {
    info("Terminating Adapter worker")

    val system = injector.instance[ActorSystem]
    system.terminate()

    val sns = injector.instance[AmazonSNS]
    val snsConfig = injector.instance[SNSConfig]
    val snsResponse = sns.publish(
      snsConfig.topicArn,
      JsonUtil
        .toJson(
          ECSServiceScheduleRequest("service_cluster", "calm_adapter", 0))
        .get)
    info(s"Sent SNS shutdown request; received ${snsResponse}")
  }
}
