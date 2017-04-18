package uk.ac.wellcome.platform.transformer.modules

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration.Duration

object KinesisWorker extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Starting Kinesis worker")

    val system = injector.instance[ActorSystem]

    val recordProcessFactory = injector.instance[StreamsRecordProcessorFactory]
    val kinesisConfig = injector
      .instance[KinesisClientLibConfiguration]
      .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
    val adapter = injector.instance[AmazonKinesis]
    val dynamoDBClient = injector.instance[AmazonDynamoDB]
    val cloudWatchClient = injector.instance[AmazonCloudWatch]

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS),
      new Worker(
        recordProcessFactory,
        kinesisConfig,
        adapter,
        dynamoDBClient,
        cloudWatchClient
      )
    )

  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Kinesis worker")

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
