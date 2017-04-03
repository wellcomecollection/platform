package uk.ac.wellcome.platform.transformer.modules

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions._
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.modules.{AkkaModule, DynamoConfigModule, SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.models.aws.DynamoConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object KinesisWorker extends TwitterModule {
  override val modules = Seq(StreamsRecordProcessorFactoryModule,
                             KinesisClientLibConfigurationModule,
                             DynamoConfigModule,
                             AkkaModule,
                             SNSConfigModule,
                             SNSClientModule)

  override def singletonStartup(injector: Injector) {
    info("Starting Kinesis worker")

    val region = injector.instance[DynamoConfig].region
    val system = injector.instance[ActorSystem]

    val adapter = new AmazonDynamoDBStreamsAdapterClient(
      new DefaultAWSCredentialsProviderChain()
    )

    adapter.setRegion(RegionUtils.getRegion(region))

    val kinesisConfig = injector
      .instance[KinesisClientLibConfiguration]
      .withInitialPositionInStream(InitialPositionInStream.LATEST)

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS),
      new Worker(
        injector.instance[StreamsRecordProcessorFactory],
        kinesisConfig,
        adapter,
        AmazonDynamoDBClientBuilder
          .standard()
          .withRegion(region)
          .build(),
        AmazonCloudWatchClientBuilder
          .standard()
          .withRegion(region)
          .build()
      )
    )

  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Kinesis worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
