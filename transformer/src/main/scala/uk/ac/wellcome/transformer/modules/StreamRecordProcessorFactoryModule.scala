package uk.ac.wellcome.platform.transformer.modules

import java.util.{List => JList}
import javax.inject.Singleton

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.amazonaws.services.kinesis.model.Record
import com.google.inject.Provides
import com.twitter.inject.{Logging, TwitterModule}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.transformer.receive.RecordReceiver

import scala.collection.JavaConverters._

class StreamsRecordProcessor(client: AmazonDynamoDB, receiver: RecordReceiver)
    extends IRecordProcessor
    with Logging {

  case class ExampleRecord(identifier: String)

  override def initialize(shardId: String) = Unit
  override def shutdown(
    checkpointer: IRecordProcessorCheckpointer,
    reason: ShutdownReason
  ): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
  }

  override def processRecords(
    records: JList[Record],
    checkpointer: IRecordProcessorCheckpointer
  ): Unit = {
    info(s"Processing records $records")
    records.asScala.foreach { (record: Record) =>
      receiver.receiveRecord(record.asInstanceOf[RecordAdapter])
    }
  }
}

class StreamsRecordProcessorFactory(
  dynamoClient: AmazonDynamoDB,
  recordReceiver: RecordReceiver
) extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =
    new StreamsRecordProcessor(dynamoClient, recordReceiver)

}

object StreamsRecordProcessorFactoryModule extends TwitterModule {
  override val modules = Seq(DynamoClientModule)

  @Singleton
  @Provides
  def provideStreamsRecordProcessorFactory(
    dynamoClient: AmazonDynamoDB,
    recordReceiver: RecordReceiver
  ): StreamsRecordProcessorFactory =
    new StreamsRecordProcessorFactory(dynamoClient, recordReceiver)
}
