package uk.ac.wellcome.transformer.utils

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.transformer.modules.KinesisClientLibConfigurationModule
import uk.ac.wellcome.test.utils.IntegrationTestBase

trait TransformerIntegrationTest extends IntegrationTestBase
  with Eventually
  with IntegrationPatience {

  object LocalKinesisClientLibConfigurationModule extends TwitterModule {
    @Provides
    @Singleton
    def provideKinesisClientLibConfiguration(dynamoConfig: DynamoConfig): KinesisClientLibConfiguration =
      KinesisClientLibConfigurationModule.provideKinesisClientLibConfiguration(dynamoConfig).withMetricsLevel(MetricsLevel.NONE)
  }
}
