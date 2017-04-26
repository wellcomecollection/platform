package uk.ac.wellcome.transformer.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{IntegrationTest, TwitterModule}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.{DynamoDBLocal, SNSLocal}

trait TransformerIntegrationTest
    extends IntegrationTest
    with SNSLocal
    with DynamoDBLocal
    with Eventually
    with IntegrationPatience {

  override val topicName: String = "test_id_minter"

  object LocalKinesisModule extends TwitterModule {

    @Provides
    @Singleton
    def provideAmazonKinesis: AmazonKinesis = {
      new AmazonDynamoDBStreamsAdapterClient(streamsClient)
    }
  }

  object LocalKinesisClientLibConfigurationModule extends TwitterModule {
    @Provides
    @Singleton
    def provideKinesisClientLibConfiguration(
      dynamoConfig: DynamoConfig): KinesisClientLibConfiguration =
      new KinesisClientLibConfiguration(
        dynamoConfig.applicationName,
        dynamoConfig.arn,
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials("access", "secret")),
        java.util.UUID.randomUUID.toString
      ) //turn off metric logging in tests so we don't see error logs about not being able to publish to cloudwatch
        .withMetricsLevel(MetricsLevel.NONE)
  }
}
