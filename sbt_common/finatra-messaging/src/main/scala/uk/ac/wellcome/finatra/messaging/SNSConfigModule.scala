package uk.ac.wellcome.finatra.messaging

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.sns.SNSConfig

object SNSConfigModule extends TwitterModule {
  private val topicArn =
    flag[String]("aws.sns.topic.arn", "", "ARN of the SNS topic")

  @Singleton
  @Provides
  def providesSNSConfig(): SNSConfig = SNSConfig(topicArn())
}
