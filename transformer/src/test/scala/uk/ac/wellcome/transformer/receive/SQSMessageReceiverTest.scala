package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.fasterxml.jackson.core.JsonParseException
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.ShouldNotTransformException
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.{CalmParser, MiroParser}
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class SQSMessageReceiverTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with TransformableSQSMessageUtils {

  val calmSqsMessage: SQSMessage = createValidCalmSQSMessage(
    "abcdef",
    "collection",
    "AB/CD/12",
    "AB/CD/12",
    """{"foo": ["bar"], "AccessStatus": ["restricted"]}""")


  val invalidCalmSqsMessage: SQSMessage = createInvalidRecord

  val failingTransformCalmSqsMessage: SQSMessage = createValidCalmSQSMessage(
    "abcdef",
    "collection",
    "AB/CD/12",
    "AB/CD/12",
    """not a json string""")

  val failingTransformMiroSqsMessage: SQSMessage =
    createValidMiroSQSMessage("""{}""")

  val work = Work(identifiers =
                    List(SourceIdentifier(IdentifierSchemes.calmPlaceholder, "value")),
                  title = "placeholder title for a Calm record")

  val metricsSender: MetricsSender = new MetricsSender(
    namespace = "record-receiver-tests",
    mock[AmazonCloudWatch])

  it("should receive a message and send it to SNS client") {
    val snsWriter = mockSNSWriter
    val recordReceiver =
      new SQSMessageReceiver(snsWriter, new CalmParser, metricsSender)
    val future = recordReceiver.receiveMessage(calmSqsMessage)

    whenReady(future) { _ =>
      verify(snsWriter).writeMessage(JsonUtil.toJson(work).get, Some("Foo"))
    }
  }

  it("should return a failed future if it's unable to parse the SQS message") {
    val recordReceiver =
      new SQSMessageReceiver(mockSNSWriter, new CalmParser, metricsSender)

    val future = recordReceiver.receiveMessage(invalidCalmSqsMessage)

    whenReady(future.failed) { x =>
      x shouldBe a [JsonParseException]
    }
  }

  it(
    "should return a failed future if it's unable to transform the transformable object") {
    val recordReceiver =
      new SQSMessageReceiver(mockSNSWriter,
        new CalmParser,
                         metricsSender)

    val future = recordReceiver.receiveMessage(failingTransformCalmSqsMessage)

    whenReady(future.failed) { x =>
      x shouldBe a [JsonParseException]
    }
  }

  it(
    "should return a successful future if it meets a ShouldNotTransformException") {
    val recordReceiver =
      new SQSMessageReceiver(mockSNSWriter,
        new MiroParser,
        metricsSender)

    val future = recordReceiver.receiveMessage(failingTransformMiroSqsMessage)

    whenReady(future) { x =>
      // We expect a `Left` here as the Transform failed
      val outerException = x.id.left.get

      outerException shouldBe a [ShouldNotTransformException]
    }
  }

  it("should return a failed future if it's unable to publish the unified item") {
    val mockSNS = mockFailPublishMessage
    val recordReceiver =
      new SQSMessageReceiver(mockSNS, new CalmParser, metricsSender)

    val future = recordReceiver.receiveMessage(calmSqsMessage)

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
