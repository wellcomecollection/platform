package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SQSConfig

object SQSConfigModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")
  private val queueUrl =
    flag[String]("aws.sqs.queue.url", "", "URL of the SQS Queue")

  @Singleton
  @Provides
  def providesSQSConfig(): SQSConfig = SQSConfig(region(), queueUrl())
}
