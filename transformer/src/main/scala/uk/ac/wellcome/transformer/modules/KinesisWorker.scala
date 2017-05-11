package uk.ac.wellcome.platform.transformer.modules

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration.Duration

object KinesisWorker extends TwitterModule {
  private var maybeWorker: Option[Worker] = None

  override def singletonStartup(injector: Injector) {
    info("Starting Kinesis worker")

    val system = injector.instance[ActorSystem]

    val recordProcessFactory = injector.instance[StreamsRecordProcessorFactory]
    val kinesisConfig = injector
      .instance[KinesisClientLibConfiguration]
      //InitialPositionInStream.LATEST won't read items added to the stream before the transformer was started,
      // hence TRIM_HORIZON
      .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
    val adapter = injector.instance[AmazonKinesis]
    val dynamoDBClient = injector.instance[AmazonDynamoDB]
    val cloudWatchClient = injector.instance[AmazonCloudWatch]

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS), {
        val worker = new Worker(

          recordProcessFactory,
          kinesisConfig,
          adapter,
          dynamoDBClient,
          cloudWatchClient
        )
        maybeWorker = Some(worker)
        worker
      }
    )

  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Kinesis worker")

    val system = injector.instance[ActorSystem]
    maybeWorker.fold(())(worker => worker.shutdown())
    system.terminate()
  }
}
