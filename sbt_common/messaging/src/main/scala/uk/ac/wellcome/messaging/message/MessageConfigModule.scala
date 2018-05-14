package uk.ac.wellcome.messaging.message

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.storage.s3.S3Config
import scala.concurrent.duration._

object MessageConfigModule extends TwitterModule {
  private val topicArn = flag[String](
    "aws.message.sns.topic.arn",
    "",
    "ARN of the SNS topic used for messaging")

  private val bucketName =
    flag[String](
      "aws.message.s3.bucketName",
      "",
      "Name of the S3 bucket holding messaging pointers")

  private val queueUrl =
    flag[String]("aws.message.sqs.queue.url", "", "URL of the SQS Queue")
  val waitTime = flag(
    "aws.message.sqs.waitTime",
    20,
    "Time to wait (in seconds) for a message to arrive on the queue before returning")
  val maxMessages =
    flag(
      "aws.message.sqs.maxMessages",
      10,
      "Maximum number of SQS messages to return")
  val parallelism =
    flag(
      "aws.message.sqs.parallelism",
      10,
      "How many messages to process at once")

  @Singleton
  @Provides
  def providesMessageWriterConfig(): MessageWriterConfig = {
    val snsConfig = SNSConfig(topicArn = topicArn())
    val s3Config = S3Config(bucketName = bucketName())
    MessageWriterConfig(snsConfig = snsConfig, s3Config = s3Config)
  }

  @Singleton
  @Provides
  def providesMessageReaderConfig(): MessageReaderConfig = {
    val sqsConfig = SQSConfig(
      queueUrl = queueUrl(),
      waitTime = waitTime() seconds,
      maxMessages = maxMessages(),
      parallelism = parallelism()
    )
    val s3Config = S3Config(bucketName = bucketName())
    MessageReaderConfig(sqsConfig = sqsConfig, s3Config = s3Config)
  }
}
