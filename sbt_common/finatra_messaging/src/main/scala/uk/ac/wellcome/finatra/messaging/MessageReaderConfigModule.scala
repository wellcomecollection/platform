package uk.ac.wellcome.finatra.messaging

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.message.MessageReaderConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.storage.s3.S3Config

import scala.concurrent.duration._

object MessageReaderConfigModule extends TwitterModule {
  private val readerBucketName =
    flag[String](
      "aws.message.reader.s3.bucketName",
      "Name of the S3 bucket where message bodies are read from")
  private val readerQueueUrl =
    flag[String](
      "aws.message.reader.sqs.queue.url",
      "URL of the SQS Queue to read messages from")
  private val readerWaitTime = flag(
    "aws.message.reader.sqs.waitTime",
    20,
    "Time to wait (in seconds) for a message to arrive on the queue before returning")
  private val readerMaxMessages =
    flag(
      "aws.message.reader.sqs.maxMessages",
      10,
      "Maximum number of SQS messages to return")
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
      waitTime = readerWaitTime() seconds,
      maxMessages = readerMaxMessages(),
      parallelism = readerParallelism()
    )
    val s3Config = S3Config(bucketName = readerBucketName())

    MessageReaderConfig(sqsConfig = sqsConfig, s3Config = s3Config)
  }
}
