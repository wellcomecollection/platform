package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.collection.JavaConversions._

trait SQSLocal
    extends BeforeAndAfterEach
    with Eventually
    with IntegrationPatience { this: Suite =>

  val sqsClient = AmazonSQSClientBuilder
    .standard()
    .withCredentials(
      new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(
      new EndpointConfiguration(s"http://localhost:9324", "localhost"))
    .build()

  def queueName: String

  // Use eventually to allow some time for the local SQS to start up.
  // If it is not started all suites using this will crash at start up time.
  val queueUrl = eventually {
    sqsClient.createQueue(queueName).getQueueUrl
  }

  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "1"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queueUrl))
  }

  object SQSLocalClientModule extends TwitterModule {

    @Singleton
    @Provides
    def providesAmazonSQSClient: AmazonSQS = sqsClient
  }
}
