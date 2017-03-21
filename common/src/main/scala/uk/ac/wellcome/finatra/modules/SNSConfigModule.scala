package uk.ac.wellcome.platform.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule


case class SNSConfig(region: String, topicArn: String)

object SNSConfigModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")
  private val topicArn = flag[String]("aws.sns.topic.arn", "", "ARN of the SNS topic")

  @Singleton
  @Provides
  def providesSNSConfig(): SNSConfig = SNSConfig(region(), topicArn())
}
