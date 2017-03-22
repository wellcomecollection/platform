package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

import uk.ac.wellcome.finatra.modules.{
  DynamoConfigModule
}

object KinesisClientLibConfigurationModule extends TwitterModule {
  override val modules = Seq(DynamoConfigModule)

  @Singleton
  @Provides
  def provideKinesisClientLibConfiguration(
    dynamoConfig: DynamoConfig): KinesisClientLibConfiguration = {

    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )

  }
}
