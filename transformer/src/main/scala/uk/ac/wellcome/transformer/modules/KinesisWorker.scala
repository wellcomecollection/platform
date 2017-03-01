package uk.ac.wellcome.platform.transformer.modules

import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem, Props}

import java.nio.charset.{ Charset => JCharset }
import java.util.{ List => JList }

import scala.collection.JavaConverters._

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.regions._
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.amazonaws.services.kinesis.model.Record

import scala.concurrent.ExecutionContext.Implicits.global

import com.gu.scanamo.ScanamoFree

import javax.inject.{Inject, Singleton}

import com.twitter.inject.{Injector, TwitterModule}

import com.twitter.inject.Logging

// import uk.ac.wellcome.platform.transformer.lib._
// import uk.ac.wellcome.platform.transformer.services._
import uk.ac.wellcome.platform.transformer.actors._
import uk.ac.wellcome.platform.transformer.modules._


object KinesisWorker extends TwitterModule {
  override val modules = Seq(
    StreamsRecordProcessorFactoryModule,
    KinesisClientLibConfigurationModule,
    DynamoConfigModule)

  val system = ActorSystem("KinesisWorker")
  val actor = system.actorOf(Props[KinesisDynamoRecordExtractorActor], name="kdreactor")

  override def singletonStartup(injector: Injector) {
    info("Starting Kinesis worker")

    val region = injector.instance[DynamoConfig].region

    val adapter = new AmazonDynamoDBStreamsAdapterClient(
      new DefaultAWSCredentialsProviderChain()
    )
    // TODO: weird stuff with Region[s]. Understand what's going on.
    // Should be able to do Regions.US_WEST_2
    adapter.setRegion(RegionUtils.getRegion(region))

    val kinesisConfig = injector.instance[KinesisClientLibConfiguration].withInitialPositionInStream(InitialPositionInStream.LATEST)

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
    info("Shutting down Kinesis worker")
    system.terminate()
  }
}