package uk.ac.wellcome.platform.idminter

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.scalatest.Suite

trait SQSLocal {

  val sqsClient = AmazonSQSClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(new EndpointConfiguration(s"http://localhost:4444", "localhost")).build()

  val idMinterQueueUrl = sqsClient.createQueue("es_id_minter_queue").getQueueUrl.replace("9324", "4444")
  val ingesterQueueUrl = sqsClient.createQueue("es_ingester_queue").getQueueUrl.replace("9324", "4444")
}
