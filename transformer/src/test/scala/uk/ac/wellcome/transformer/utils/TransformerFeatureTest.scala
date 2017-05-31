package uk.ac.wellcome.transformer.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{
  AmazonCloudWatch,
  AmazonCloudWatchClientBuilder
}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.amazonaws.services.sns.AmazonSNS
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.Suite
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.platform.transformer.Server
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchLocal,
  DynamoDBLocal,
  ExtendedPatience,
  SNSLocal
}

trait TransformerFeatureTest
    extends FeatureTestMixin
    with ExtendedPatience
    with Eventually
    with SNSLocal
    with DynamoDBLocal
    with AmazonCloudWatchLocal { this: Suite =>
  val flags: Map[String, String]
  val kinesisClientLibConfiguration: KinesisClientLibConfiguration

  lazy val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags
    ).bind[AmazonSNS](amazonSNS)
      .bind[AmazonDynamoDB](dynamoDbClient)
      .bind[AmazonCloudWatch](amazonCloudWatch)
      .bind[AmazonKinesis](new AmazonDynamoDBStreamsAdapterClient(
        streamsClient))
      .bind[KinesisClientLibConfiguration](kinesisClientLibConfiguration)

  val idMinterTopicArn: String = createTopicAndReturnArn("test_id_minter")

  def kinesisClientLibConfiguration(
    applicationName: String,
    streamArn: String): KinesisClientLibConfiguration =
    new KinesisClientLibConfiguration(
      applicationName,
      streamArn,
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials("access", "secret")),
      java.util.UUID.randomUUID.toString)
    //turn off metric logging in tests so we don't see error logs about not being able to publish to cloudwatch
      .withMetricsLevel(MetricsLevel.NONE)

}
