package uk.ac.wellcome.platform.transformer.modules

import com.twitter.inject.TwitterModule
import com.google.inject.Provides
import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.platform.transformer.modules._
// import uk.ac.wellcome.platform.transformer.services._

import javax.inject.{Inject, Singleton}

import com.twitter.inject.TwitterModule

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

import com.google.inject.Provides

import scala.concurrent.ExecutionContext.Implicits.global

import com.gu.scanamo.ScanamoFree

import com.twitter.inject.{Injector, TwitterModule}


class StreamsRecordProcessor(client: AmazonDynamoDB) extends IRecordProcessor {

  case class ExampleRecord(identifier: String)

  override def initialize(shardId: String) = Unit
  override def shutdown(checkpointer: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
  }

  override def processRecords(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    records.asScala.map { record =>
      if (record.isInstanceOf[RecordAdapter]) {
        val recordAdapter = record.asInstanceOf[RecordAdapter]
        val keys = recordAdapter
          .getInternalObject()
          .getDynamodb()
          .getKeys()
        // TODO: proper pattern matching and map on result
        val newRecord: ExampleRecord = ScanamoFree.read[ExampleRecord](keys).right.get
        println(newRecord)
      }
    }
  }
}



class StreamsRecordProcessorFactory(dynamoConfig: DynamoConfig) extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = {
    new StreamsRecordProcessor(
      AmazonDynamoDBClientBuilder
        .standard()
        .withRegion(dynamoConfig.region)
        .build())
    }
}


object StreamsRecordProcessorFactoryModule extends TwitterModule {
  override val modules = Seq(DynamoConfigModule)

  @Singleton
  @Provides
  def provideStreamsRecordProcessorFactory(dynamoConfig: DynamoConfig): StreamsRecordProcessorFactory = {
    new StreamsRecordProcessorFactory(dynamoConfig)
  }
}
