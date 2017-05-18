package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.annotations.{CalmDynamoConfig, MiroDynamoConfig}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.transformer.annotations.{
  CalmKinesisClientLibConfiguration,
  MiroKinesisClientLibConfiguration
}

object KinesisClientLibConfigurationModule extends TwitterModule {

  @Singleton
  @Provides
  @CalmKinesisClientLibConfiguration
  def provideCalmKinesisClientLibConfiguration(
    @CalmDynamoConfig dynamoConfig: DynamoConfig)
    : KinesisClientLibConfiguration = {

    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )

  }

  @Singleton
  @Provides
  @MiroKinesisClientLibConfiguration
  def provideMiroKinesisClientLibConfiguration(
    @MiroDynamoConfig dynamoConfig: DynamoConfig)
    : KinesisClientLibConfiguration = {

    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )

  }
}
