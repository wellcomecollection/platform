package uk.ac.wellcome.sqs

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class SQSMessageReceiverTest
    extends FunSpec
    with Eventually
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience {

  val metricsSender: MetricsSender = new MetricsSender(
    namespace = "test-SQSMessageReceiver",
    mock[AmazonCloudWatch]
  )

  it("should send a successfully processed message to the SNS client") {
    val snsWriter = mockSNSWriter
    val recordReceiver = new SQSMessageReceiver(
      snsWriter = snsWriter,
      messageProcessor = simpleMessageProcessor,
      metricsSender = metricsSender,
      snsSubject = Some("marine life")
    )

    whenReady(recordReceiver.receiveMessage(simpleMessage)) { _ =>
      verify(snsWriter).writeMessage(
        JsonUtil.toJson(List(simpleMessage.body)).get,
        Some("marine life")
      )
    }
  }

  val simpleMessage = SQSMessage(
    subject = Some("Marine creatures beginning with 'C'"),
    body = "A collection of clownfish",
    topic = "arn:::sqs:::marine_creatures_beginning_with_c",
    messageType = "text",
    timestamp = "now"
  )

  def simpleMessageProcessor(message: SQSMessage): Try[Any] =
    Success(List(message.body))

  private def mockSNSWriter = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[Option[String]]))
      .thenReturn(Future { PublishAttempt(Right("1234")) })
    mockSNS
  }
}
