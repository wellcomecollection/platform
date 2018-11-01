package uk.ac.wellcome.finatra.messaging

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.sqs.SQSConfig

object SQSConfigModule extends TwitterModule {
  private val queueUrl = flag[String](
    name = "aws.sqs.queue.url",
    help = "URL of the SQS Queue"
  )
  val maxMessages =
    flag("aws.sqs.maxMessages", 10, "Maximum number of SQS messages to return")

  val parallelism =
    flag("aws.sqs.parallelism", 10, "Number of messages to process in parallel")

  @Singleton
  @Provides
  def providesSQSConfig(): SQSConfig =
    SQSConfig(queueUrl(), maxMessages(), parallelism())
}
