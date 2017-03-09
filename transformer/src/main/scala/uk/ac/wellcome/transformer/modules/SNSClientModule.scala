package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain


case class WorkerConfig(snsTopicArn: String)

object WorkerConfigModule extends TwitterModule {
  private val snsTopicArn = flag[String]("sns.topic.arn", "", "SNS Topic ARN")

  @Singleton
  @Provides
  def providesWorkerConfig(): WorkerConfig =
    WorkerConfig(snsTopicArn())

}

object SNSClientModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")

  @Singleton
  @Provides
  def providesSNSClient(): AmazonSNS =
    AmazonSNSClientBuilder
      .standard()
      .withRegion(region())
      .withCredentials(
        new DefaultAWSCredentialsProviderChain())
      .build()

}
