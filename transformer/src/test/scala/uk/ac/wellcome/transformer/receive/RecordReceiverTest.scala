package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, Record, StreamRecord}
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{SourceIdentifier, Transformable, UnifiedItem}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.Try

class RecordReceiverTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience {

  it("should receive a message and send it to SNS client") {
    val record = mockRecord
    val sNSWriter = mockSNSWriter
    val unifiedItem =
      UnifiedItem(List(SourceIdentifier("source", "key", "value")),
        Some("TopSekrit"))
    val recordReceiver = new RecordReceiver(sNSWriter, mockTransformableParser(record, unifiedItem))
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(record))

    whenReady(future) { _ =>
      Mockito
        .verify(sNSWriter)
        .writeMessage(UnifiedItem.json(unifiedItem), Some("Foo"))
    }
  }

  it("should return a failed future if it's unable to parse the dynamo record") {
    val invalidRecord = createInvalidRecord
    val recordReceiver = new RecordReceiver(mockSNSWriter, mockFailingTransformableParser(invalidRecord))
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(invalidRecord))

    whenReady(future.failed) { x =>
      x.getMessage should startWith("Unable to parse transformable")
    }
  }

  it("should return a failed future if it's unable to publish the unified item") {
    val mockSNS = mockFailPublishMessage
    val recordReceiver = new RecordReceiver(mockSNS, mockTransformableParser(mockRecord, UnifiedItem(List(SourceIdentifier("Calm", "AltRefNo", "1234")), None)))
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(mockRecord))

    whenReady(future.failed) { x =>
      x.getMessage should be("Failed publishing message")
    }
  }

  private def mockSNSWriter = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(Future { PublishAttempt("1234") })
    mockSNS
  }

  private def mockTransformableParser(record: Record, unifiedItem: UnifiedItem) = {
    val transformableParser = mock[TransformableParser]
    val recordMap = RecordMap(record.getDynamodb.getNewImage)
    when(transformableParser.extractTransformable(recordMap)).thenReturn(Try {
      new Transformable {
        override def transform: Try[UnifiedItem] = Try {
          unifiedItem
        }
      }
    })
    transformableParser
  }

  private def mockFailingTransformableParser(record: Record) = {
    val transformableParser = mock[TransformableParser]
    val recordMap = RecordMap(record.getDynamodb.getNewImage)
    when(transformableParser.extractTransformable(recordMap))
      .thenThrow(new RuntimeException("Unable to parse transformable"))
    transformableParser
  }

  private def mockFailPublishMessage = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(Future.failed(new RuntimeException("Failed publishing message")))
    mockSNS
  }

  private def createNonTransformableRecord = {
    val record = new Record()
    val streamRecord = createStreamRecord("""{"AccessStatus":"not a list"}""")
    record.withDynamodb(streamRecord)
    record
  }

  private def mockRecord = {
    val record = new Record()
    val streamRecord = createStreamRecord(
      """{"foo": ["bar"], "AccessStatus": ["TopSekrit"]}""")
    record.withDynamodb(streamRecord)
    record
  }

  private def createStreamRecord(data: String) = {
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("RecordID", new AttributeValue("abcdef"))
    streamRecord.addNewImageEntry("RecordType",
                                  new AttributeValue("collection"))
    streamRecord.addNewImageEntry("RefNo", new AttributeValue("AB/CD/12"))
    streamRecord.addNewImageEntry("AltRefNo", new AttributeValue("AB/CD/12"))
    streamRecord.addNewImageEntry("data", new AttributeValue(data))
    streamRecord
  }

  private def createInvalidRecord = {
    val record = new Record()
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("something",
                                  new AttributeValue("something-else"))
    record.withDynamodb(streamRecord)
    record
  }
}
