package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.{ListQueuesResult, PurgeQueueRequest}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, Suite}

import scala.collection.JavaConversions._

trait SQSLocal
    extends BeforeAndAfterEach
    with Eventually
    with ExtendedPatience
    with Matchers { this: Suite =>

  val sqsClient: AmazonSQS = AmazonSQSClientBuilder
    .standard()
    .withCredentials(
      new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(
      new EndpointConfiguration(s"http://localhost:9324", "localhost"))
    .build()

  // Use eventually to allow some time for the local SQS to start up.
  // If it is not started all suites using this will crash at start up time.
  eventually {
    sqsClient.listQueues() shouldBe a[ListQueuesResult]
  }

  private var queueUrls: List[String] = Nil

  def createQueueAndDlqReturnUrls(queueName: String): QueueInfo = {
    val deadLetterQueueUrl = sqsClient.createQueue(queueName).getQueueUrl
    val queueUrl = sqsClient.createQueue(s"$queueName-dlq").getQueueUrl
    val deadLetterQueueArn = sqsClient
      .getQueueAttributes(deadLetterQueueUrl, List("QueueArn"))
      .getAttributes
      .get("QueueArn")
    val redrivePolicy =
      s"""
        |{
        | "maxReceiveCount":"1",
        | "deadLetterTargetArn":"$deadLetterQueueArn"
        |}""".stripMargin
    sqsClient.setQueueAttributes(queueUrl,
                                 Map("RedrivePolicy" -> redrivePolicy))
    queueUrls = queueUrl :: queueUrls
    QueueInfo(queueUrl, deadLetterQueueUrl)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    queueUrls.foreach(queueUrl =>
      sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queueUrl)))
  }
}

case class QueueInfo(queueUrl: String, deadLetterQueueUrl: String)
