package uk.ac.wellcome.platform.transformer.modules

import java.util.{List => JList}

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.amazonaws.services.kinesis.model.Record
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.transformer.receive.RecordReceiver

import scala.collection.JavaConverters._

class StreamsRecordProcessor(receiver: RecordReceiver)
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

class StreamsRecordProcessorFactory @Inject()(
  recordReceiver: RecordReceiver
) extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =
    new StreamsRecordProcessor(recordReceiver)

}
