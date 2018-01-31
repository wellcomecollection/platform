package uk.ac.wellcome.test.utils

import org.scalatest.Suite
import scala.collection.JavaConversions._

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder,
  AmazonDynamoDBStreams,
  AmazonDynamoDBStreamsClientBuilder
}

trait DynamoDBLocalClients { this: Suite =>
  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val accessKey = "access"
  private val secretKey = "secret"
  val dynamoDbLocalEndpointFlags: Map[String, String] =
    Map(
      "aws.dynamoDb.endpoint" -> dynamoDBEndPoint,
      "aws.region" -> "localhost",
      "aws.dynamoDb.accessKey" -> accessKey,
      "aws.dynamoDb.secretKey" -> secretKey
    )

  private val dynamoDBLocalCredentialsProvider =
    new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(accessKey, secretKey))

  val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  val streamsClient: AmazonDynamoDBStreams = AmazonDynamoDBStreamsClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()
}
