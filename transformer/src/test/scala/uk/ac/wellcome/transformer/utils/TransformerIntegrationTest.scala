package uk.ac.wellcome.transformer.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.IntegrationTestBase

trait TransformerIntegrationTest extends IntegrationTestBase
  with Eventually
  with IntegrationPatience {

  object LocalKinesisClientLibConfigurationModule extends TwitterModule {
    @Provides
    @Singleton
    def provideKinesisClientLibConfiguration(dynamoConfig: DynamoConfig): KinesisClientLibConfiguration =
      new KinesisClientLibConfiguration(
        dynamoConfig.applicationName,
        dynamoConfig.arn,
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials("access", "secret")),
        java.util.UUID.randomUUID.toString
      ).withMetricsLevel(MetricsLevel.NONE)
  }
}
