package uk.ac.wellcome.messaging.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SNSConfig

object SNSConfigModule extends TwitterModule {
  private val topicArn =
    flag[String]("aws.sns.topic.arn", "", "ARN of the SNS topic")

  @Singleton
  @Provides
  def providesSNSConfig(): SNSConfig = SNSConfig(topicArn())
}
