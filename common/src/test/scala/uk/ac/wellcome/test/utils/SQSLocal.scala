package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SQSLocal
    extends BeforeAndAfterEach
    with Eventually
    with ExtendedPatience { this: Suite =>

  private val sqsEndpointUrl = s"http://localhost:9324"

  val sqsLocalFlags = Map(
    "aws.sqs.endpoint" -> sqsEndpointUrl,
    "aws.accessKey" -> "access",
    "aws.secretKey" -> "secret"
  )

  val sqsClient: AmazonSQS = AmazonSQSClientBuilder
    .standard()
    .withCredentials(
      new AWSStaticCredentialsProvider(new BasicAWSCredentials("access", "secret")))
    .withEndpointConfiguration(
      new EndpointConfiguration(sqsEndpointUrl, "localhost"))
    .build()

  private var queueUrls: List[String] = Nil

  def createQueueAndReturnUrl(queueName: String): String = {
    // Use eventually to allow some time for the local SQS to start up.
    // If it is not started all suites using this will crash at start up time.
    val queueUrl = eventually {
      sqsClient.createQueue(queueName).getQueueUrl
    }
    queueUrls = queueUrl :: queueUrls
    queueUrl
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    queueUrls.foreach(queueUrl => sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queueUrl)))
  }
}
