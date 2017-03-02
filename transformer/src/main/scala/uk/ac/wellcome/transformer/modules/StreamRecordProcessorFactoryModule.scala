package uk.ac.wellcome.platform.transformer.modules

import java.util.{ List => JList }
import javax.inject.Singleton
import scala.collection.JavaConverters._

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.amazonaws.services.kinesis.model.Record
import com.google.inject.Provides
import com.twitter.inject.TwitterModule

import uk.ac.wellcome.platform.transformer.modules._


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
      KinesisWorker.kinesisDynamoRecordExtractorActor ! record }
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
