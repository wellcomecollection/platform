package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.Record
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{SourceIdentifier, Transformable, UnifiedItem}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.transformer.utils.CalmRecordUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.Try

class RecordReceiverTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with CalmRecordUtils {

  val calmRecord = createValidCalmRecord(
    "abcdef",
    "collection",
    "AB/CD/12",
    "AB/CD/12",
    """{"foo": ["bar"], "AccessStatus": ["TopSekrit"]}""")

  val unifiedItem = UnifiedItem(
    List(SourceIdentifier("Calm", "AltRefNo", "AB/CD/12")),
    None,
    Some("TopSekrit"))

  it("should receive a message and send it to SNS client") {
    val sNSWriter = mockSNSWriter
    val recordReceiver =
      new RecordReceiver(sNSWriter,
                         transformableParser(calmRecord, unifiedItem))
    val future = recordReceiver.receiveRecord(new RecordAdapter(calmRecord))

    whenReady(future) { _ =>
      verify(sNSWriter).writeMessage(UnifiedItem.json(unifiedItem),
                                     Some("Foo"))
    }
  }

  it("should return a failed future if it's unable to parse the dynamo record") {
    val recordReceiver =
      new RecordReceiver(mockSNSWriter, failingParser(calmRecord))
    val future = recordReceiver.receiveRecord(new RecordAdapter(calmRecord))

    whenReady(future.failed) { x =>
      x.getMessage should startWith("Unable to parse transformable")
    }
  }

  it("should return a failed future if it's unable to transform the transformable object") {
    val recordReceiver =
      new RecordReceiver(mockSNSWriter,
                         parserReturningFailingTransformable(calmRecord))
    val future = recordReceiver.receiveRecord(new RecordAdapter(calmRecord))

    whenReady(future.failed) { x =>
      x.getMessage should startWith("Unable to transform into unified item")
    }
  }

  it("should return a failed future if it's unable to publish the unified item") {
    val mockSNS = mockFailPublishMessage
    val recordReceiver =
      new RecordReceiver(mockSNS, transformableParser(calmRecord, unifiedItem))
    val future = recordReceiver.receiveRecord(new RecordAdapter(calmRecord))

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

  private def mockFailPublishMessage = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(
        Future.failed(new RuntimeException("Failed publishing message")))
    mockSNS
  }

  private def transformableParser(record: Record, unifiedItem: UnifiedItem) = {
    val transformableParser = mock[TransformableParser[Transformable]]
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

  private def parserReturningFailingTransformable(record: Record) = {
    val transformableParser = mock[TransformableParser[Transformable]]
    val recordMap = RecordMap(record.getDynamodb.getNewImage)
    when(transformableParser.extractTransformable(recordMap)).thenReturn(Try {
      new Transformable {
        override def transform: Try[UnifiedItem] = Try {
          throw new RuntimeException("Unable to transform into unified item")
        }
      }
    })
    transformableParser
  }

  private def failingParser(record: Record) = {
    val transformableParser = mock[TransformableParser[Transformable]]
    val recordMap = RecordMap(record.getDynamodb.getNewImage)
    when(transformableParser.extractTransformable(recordMap))
      .thenThrow(new RuntimeException("Unable to parse transformable"))
    transformableParser
  }
}
