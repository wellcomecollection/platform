package uk.ac.wellcome.platform.transformer.modules

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.twitter.inject.TwitterModule
import javax.inject.{Inject, Singleton}
import com.google.inject.Provides
import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker._


object KinesisClientLibConfigurationModule extends TwitterModule {
  override val modules = Seq(DynamoConfigModule)

  @Singleton
  @Provides
  def provideKinesisClientLibConfiguration(dynamoConfig: DynamoConfig): KinesisClientLibConfiguration = {
    new KinesisClientLibConfiguration(
      dynamoConfig.applicationName,
      dynamoConfig.arn,
      new DefaultAWSCredentialsProviderChain(),
      java.util.UUID.randomUUID.toString
    )
  }
}
