package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.{
  AttributeValue,
  Record,
  StreamRecord
}
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import com.fasterxml.jackson.databind.JsonMappingException
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.models.{Identifier, UnifiedItem}

class RecordReceiverTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience {

  val config = SNSConfig("eu-west-1", "arn:test:test:test:test:test")
  val mockSNS = createMockSNS

  it("should receive a message and send it to SNS client") {
    val recordReceiver = new RecordReceiver(config, mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createMockRecord))

    whenReady(future) { _ =>
      val unifiedItem = UnifiedItem("id",
                                    List(Identifier("source", "key", "value")),
                                    Some("TopSekrit"))
      Mockito
        .verify(mockSNS)
        .publish(
          new PublishRequest(config.topicArn,
                             UnifiedItem.json(unifiedItem),
                             "Foo"))
    }
  }

  it("should return a failed future if it's unable to parse the dynamo record") {
    val recordReceiver = new RecordReceiver(config, mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createInvalidRecord))

    whenReady(future.failed) { x =>
      x.getMessage should startWith("Unable to parse record")
    }
  }

  it("should return a failed future if it's unable to transform the parsed record") {
    val recordReceiver = new RecordReceiver(config, mockSNS)
    val future = recordReceiver.receiveRecord(
      new RecordAdapter(createNonTransformableRecord))

    whenReady(future.failed) { x =>
      x shouldBe a[JsonMappingException]
    }
  }

  it("should return a failed future if it's unable to publish the unified item") {
    val mockSNS = mockFailPublishMessage
    val recordReceiver = new RecordReceiver(config, mockSNS)
    val future =
      recordReceiver.receiveRecord(new RecordAdapter(createMockRecord))

    whenReady(future.failed) { x =>
      x.getMessage should be("Failed publishing message")
    }
  }

  private def createMockSNS = {
    val mockSNS = mock[AmazonSNS]
    val publishResult = new PublishResult()
    publishResult.setMessageId("1234")
    when(mockSNS.publish(any[PublishRequest]())).thenReturn(publishResult)
    mockSNS
  }

  private def mockFailPublishMessage = {
    val mockSNS = mock[AmazonSNS]
    when(mockSNS.publish(any[PublishRequest]()))
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
