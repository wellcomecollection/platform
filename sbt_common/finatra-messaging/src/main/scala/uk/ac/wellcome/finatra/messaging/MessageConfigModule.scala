package uk.ac.wellcome.finatra.messaging

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.message.{
  MessageReaderConfig,
  MessageWriterConfig
}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.storage.s3.S3Config

import scala.concurrent.duration._

object MessageConfigModule extends TwitterModule {
  private val writerTopicArn = flag[String](
    "aws.message.writer.sns.topic.arn",
    "",
    "ARN of the SNS topic where new message pointers are sent")
  private val writerBucketName =
    flag[String](
      "aws.message.writer.s3.bucketName",
      "",
      "Name of the S3 bucket where new message bodies are written")

  private val readerBucketName =
    flag[String](
      "aws.message.reader.s3.bucketName",
      "",
      "Name of the S3 bucket where message bodies are read from")
  private val readerQueueUrl =
    flag[String](
      "aws.message.reader.sqs.queue.url",
      "",
      "URL of the SQS Queue to read messages from")
  val waitTime = flag(
    "aws.message.sqs.waitTime",
    20,
    "Time to wait (in seconds) for a message to arrive on the queue before returning")
  val maxMessages =
    flag(
      "aws.message.sqs.maxMessages",
      10,
      "Maximum number of SQS messages to return")

  @Singleton
  @Provides
  def providesMessageWriterConfig(): MessageWriterConfig = {
    val snsConfig = SNSConfig(topicArn = writerTopicArn())
    val s3Config = S3Config(bucketName = writerBucketName())

    MessageWriterConfig(snsConfig = snsConfig, s3Config = s3Config)
  }

  @Singleton
  @Provides
  def providesMessageReaderConfig(): MessageReaderConfig = {
    val sqsConfig = SQSConfig(
      queueUrl = readerQueueUrl(),
      waitTime() seconds, maxMessages())
    val s3Config = S3Config(bucketName = readerBucketName())

    MessageReaderConfig(sqsConfig = sqsConfig, s3Config = s3Config)
  }
}
