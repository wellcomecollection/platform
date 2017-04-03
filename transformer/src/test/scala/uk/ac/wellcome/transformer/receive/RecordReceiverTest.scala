package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, Record, StreamRecord}
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.models.{Identifier, UnifiedItem}

class RecordReceiverTest extends FlatSpec with MockitoSugar with ScalaFutures{


  it should "receive a message and send it to SNS client" in {
    val mockSNS = createMockSNS
    val config = SNSConfig("eu-west-1", "arn:test:test:test:test:test")
    val recordReceiver = new RecordReceiver(config, mockSNS)

    val future = recordReceiver.receiveRecord(new RecordAdapter(createMockRecord))

    whenReady(future, timeout(Span(6, Seconds)), interval(Span(500, Millis))) {_ =>
      val unifiedItem = UnifiedItem("id", List(Identifier("source", "key", "value")), Some("TopSekrit"))
      Mockito.verify(mockSNS).publish(new PublishRequest(config.topicArn, UnifiedItem.json(unifiedItem), "Foo"))
    }
  }

  private def createMockSNS = {
    val mockSNS = mock[AmazonSNS]
    val publishResult = new PublishResult()
    publishResult.setMessageId("1234")
    when(mockSNS.publish(any[PublishRequest]())).thenReturn(publishResult)
    mockSNS
  }

  private def createMockRecord = {
    val record = new Record()
    val streamRecord = new StreamRecord()
    streamRecord.addNewImageEntry("RecordID", new AttributeValue("abcdef"))
    streamRecord.addNewImageEntry("RecordType", new AttributeValue("collection"))
    streamRecord.addNewImageEntry("RefNo", new AttributeValue("AB/CD/12"))
    streamRecord.addNewImageEntry("AltRefNo", new AttributeValue("AB/CD/12"))
    streamRecord.addNewImageEntry("data", new AttributeValue("""{"foo": ["bar"], "AccessStatus": ["TopSekrit"]}"""))
    record.withDynamodb(streamRecord)
    record
  }
}
