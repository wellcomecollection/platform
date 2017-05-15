package uk.ac.wellcome.transformer.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.Suite
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience, SNSLocal}

trait TransformerFeatureTest
    extends FeatureTestMixin
    with ExtendedPatience
    with Eventually
    with SNSLocal
    with DynamoDBLocal { this: Suite =>

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
