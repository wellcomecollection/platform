package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SQSLocal extends Suite with BeforeAndAfterEach {

  val sqsClient = AmazonSQSClientBuilder
    .standard()
    .withCredentials(
      new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(
      new EndpointConfiguration(s"http://localhost:9324", "localhost"))
    .build()

  val idMinterQueue = "es_id_minter_queue"
  val idMinterQueueUrl = sqsClient.createQueue(idMinterQueue).getQueueUrl

  override def beforeEach(): Unit = {
    super.beforeEach()
    sqsClient.purgeQueue(
      new PurgeQueueRequest().withQueueUrl(idMinterQueueUrl))
  }
}
