package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.annotations.{CalmDynamoConfig, MiroDynamoConfig}
import uk.ac.wellcome.models.aws.DynamoConfig

object KinesisClientLibConfigurationModule extends TwitterModule {

  @Singleton
  @Provides
  def provideKinesisClientLibConfiguration(
    @CalmDynamoConfig dynamoConfig: DynamoConfig): KinesisClientLibConfiguration = {

    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )

  }
}
