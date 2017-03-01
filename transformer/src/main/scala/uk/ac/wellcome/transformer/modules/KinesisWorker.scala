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

// import uk.ac.wellcome.platform.transformer.lib._
// import uk.ac.wellcome.platform.transformer.services._
import uk.ac.wellcome.platform.transformer.modules._


object KinesisWorker extends TwitterModule {
  override val modules = Seq(
    StreamsRecordProcessorFactoryModule,
    KinesisClientLibConfigurationModule)

  val system = ActorSystem("KinesisWorker")

  override def singletonStartup(injector: Injector) {
    println("@@ Hello world, I am starting")
    println("@@ I am very excited to be starting")

    val adapter = new AmazonDynamoDBStreamsAdapterClient(
      new DefaultAWSCredentialsProviderChain()
    )
    // TODO: weird stuff with Region[s]. Understand what's going on.
    // Should be able to do Regions.US_WEST_2
    adapter.setRegion(RegionUtils.getRegion("eu-west-1"))

    val kinesisConfig = injector.instance[KinesisClientLibConfiguration].withInitialPositionInStream(InitialPositionInStream.LATEST)

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS),
      new Worker(
        injector.instance[StreamsRecordProcessorFactory],
        kinesisConfig,
        adapter,
        AmazonDynamoDBClientBuilder
          .standard()
          .withRegion("eu-west-1")
          .build(),
        AmazonCloudWatchClientBuilder
          .standard()
          .withRegion("eu-west-1")
          .build()
      )
    )
  }

  override def singletonShutdown(injector: Injector) {
    println("@@ Goodbye cruel world")
  }
}