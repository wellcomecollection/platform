package uk.ac.wellcome.transformer.receive

import java.time.Instant

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SNSConfig, SQSMessage}
import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}
import uk.ac.wellcome.s3.VersionedObjectStore
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.test.utils.SNSLocal
import uk.ac.wellcome.transformer.transformers.{
  CalmTransformableTransformer,
  SierraTransformableTransformer
}
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SQSMessageReceiverTest
    extends FunSpec
    with MockitoSugar
    with SNSLocal
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with TransformableSQSMessageUtils {

  override lazy val bucketName: String = "test-sqs-message-receiver-bucket"

  val sourceIdentifier =
    SourceIdentifier(IdentifierSchemes.calmPlaceholder, "value")

  val work = Work(title = Some("placeholder title for a Calm record"),
                  sourceIdentifier = sourceIdentifier,
                  identifiers = List(sourceIdentifier))

  val metricsSender: MetricsSender = new MetricsSender(
    namespace = "record-receiver-tests",
    mock[AmazonCloudWatch]
  )
  val topicArn = createTopicAndReturnArn("test-sqs-message-retriever")
  val snsWriter = new SNSWriter(snsClient, SNSConfig(topicArn))
  private val versionedObjectStore =
    new VersionedObjectStore(s3Client, bucketName)
  val recordReceiver =
    new SQSMessageReceiver(snsWriter, versionedObjectStore, metricsSender)

  it("should receive a message and send it to SNS client") {
    val calmSqsMessage: SQSMessage = hybridRecordSqsMessage(
      createValidCalmTramsformableJson(
        RecordID = "abcdef",
        RecordType = "collection",
        AltRefNo = "AB/CD/12",
        RefNo = "AB/CD/12",
        data = """{"foo": ["bar"], "AccessStatus": ["restricted"]}"""
      ),
      "calm"
    )

    val future = recordReceiver.receiveMessage(calmSqsMessage)

    whenReady(future) { _ =>
      val messages = listMessagesReceivedFromSNS()
      messages should have size 1
      messages.head.message shouldBe JsonUtil.toJson(work).get
      messages.head.subject shouldBe "Foo"
    }
  }

  it("should return a failed future if it's unable to parse the SQS message") {
    val invalidCalmSqsMessage: SQSMessage =
      hybridRecordSqsMessage("not a json string", "calm")

    val future = recordReceiver.receiveMessage(invalidCalmSqsMessage)

    whenReady(future.failed) { x =>
      x shouldBe a[GracefulFailureException]
    }
  }

  it("should send no message where Transformable work is None") {
    val snsWriter = mockSNSWriter

    val recordReceiver =
      new SQSMessageReceiver(snsWriter, versionedObjectStore, metricsSender)

    val future = recordReceiver.receiveMessage(
      createValidEmptySierraBibSQSMessage("000")
    )

    whenReady(future) { x =>
      verify(snsWriter, Mockito.never())
        .writeMessage(anyString, any[Option[String]])
    }
  }

  it(
    "should return a failed future if it's unable to transform the transformable object") {
    val failingTransformCalmSqsMessage: SQSMessage =
      hybridRecordSqsMessage(createValidCalmTramsformableJson(
                               RecordID = "abcdef",
                               RecordType = "collection",
                               AltRefNo = "AB/CD/12",
                               RefNo = "AB/CD/12",
                               data = """not a json string"""
                             ),
                             "calm")

    val future = recordReceiver.receiveMessage(failingTransformCalmSqsMessage)

    whenReady(future.failed) { x =>
      x shouldBe a[GracefulFailureException]
    }
  }

  it("should return a failed future if it's unable to publish the work") {
    val id = "b123"
    val sierraTransformable: Transformable =
      SierraTransformable(sourceId = id,
                          bibData = JsonUtil
                            .toJson(
                              SierraBibRecord(id = id,
                                              data = s"""{"id": "$id"}""",
                                              modifiedDate = Instant.now))
                            .get)
    val message =
      hybridRecordSqsMessage(JsonUtil.toJson(sierraTransformable).get,
                             "sierra")

    val mockSNS = mockFailPublishMessage
    val recordReceiver =
      new SQSMessageReceiver(mockSNS, versionedObjectStore, metricsSender)

    val future = recordReceiver.receiveMessage(message)

    whenReady(future.failed) { x =>
      x.getMessage should be("Failed publishing message")
    }
  }

  private def mockSNSWriter = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(Future { PublishAttempt(Right("1234")) })
    mockSNS
  }

  private def mockFailPublishMessage = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(
        Future.failed(new RuntimeException("Failed publishing message")))
    mockSNS
  }
}
