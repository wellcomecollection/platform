package uk.ac.wellcome.finatra.messaging

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.message.MessageReaderConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig

object MessageReaderConfigModule extends TwitterModule {
  private val readerQueueUrl =
    flag[String](
      "aws.message.reader.sqs.queue.url",
      "URL of the SQS Queue to read messages from")
  private val readerParallelism =
    flag(
      name = "aws.message.reader.sqs.parallelism",
      default = 10,
      help = "Number of messages to process in parallel")

  @Singleton
  @Provides
  def providesMessageReaderConfig(): MessageReaderConfig = {
    val sqsConfig = SQSConfig(
      queueUrl = readerQueueUrl(),
      parallelism = readerParallelism()
    )

    MessageReaderConfig(sqsConfig = sqsConfig)
  }
}
