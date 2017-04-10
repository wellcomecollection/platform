package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.{
  AttributeValue,
  Record,
  StreamRecord
}
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.fasterxml.jackson.databind.JsonMappingException
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Identifier, UnifiedItem}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class RecordReceiverTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience {

  val mockSNS = createMockSNS

  it("should receive a message and send it to SNS client") {
    val recordReceiver = new RecordReceiver(mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createMockRecord))

    whenReady(future) { _ =>
      val unifiedItem = UnifiedItem(List(Identifier("source", "key", "value")),
                                    Some("TopSekrit"))
      Mockito
        .verify(mockSNS)
        .writeMessage(UnifiedItem.json(unifiedItem), Some("Foo"))
    }
  }

  it("should return a failed future if it's unable to parse the dynamo record") {
    val recordReceiver = new RecordReceiver(mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createInvalidRecord))

    whenReady(future.failed) { x =>
      x.getMessage should startWith("Unable to parse record")
    }
  }

  it("should return a failed future if it's unable to transform the parsed record") {
    val recordReceiver = new RecordReceiver(mockSNS)
    val future = recordReceiver.receiveRecord(
      new RecordAdapter(createNonTransformableRecord))

    whenReady(future.failed) { x =>
      x shouldBe a[JsonMappingException]
    }
  }

  it("should return a failed future if it's unable to publish the unified item") {
    val mockSNS = mockFailPublishMessage
    val recordReceiver = new RecordReceiver(mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createMockRecord))

    whenReady(future.failed) { x =>
      x.getMessage should be("Failed publishing message")
    }
  }

  private def createMockSNS = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(Future { PublishAttempt("1234") })
    mockSNS
  }

  private def mockFailPublishMessage = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenThrow(new RuntimeException("Failed publishing message"))
    mockSNS
  }

  private def createNonTransformableRecord = {
    val record = new Record()
    val streamRecord = createStreamRecord("""{"AccessStatus":"b"}""")
    record.withDynamodb(streamRecord)
    record
  }

  private def createMockRecord = {
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
