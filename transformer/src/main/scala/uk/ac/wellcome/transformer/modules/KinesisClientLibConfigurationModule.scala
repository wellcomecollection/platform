package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

object KinesisClientLibConfigurationModule extends TwitterModule {

  @Singleton
  @Provides
  def provideKinesisClientLibConfiguration(
    dynamoConfigs: Map[String, DynamoConfig])
    : KinesisClientLibConfiguration = {

    val dynamoConfig = DynamoConfig.findWithTable(dynamoConfigs.values.toList)

    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )

  }
}
