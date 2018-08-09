package uk.ac.wellcome.finatra.messaging

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.message.MessageWriterConfig
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.storage.s3.S3Config

object MessageWriterConfigModule extends TwitterModule {
  private val writerTopicArn = flag[String](
    "aws.message.writer.sns.topic.arn",
    "ARN of the SNS topic where new message pointers are sent")
  private val writerBucketName =
    flag[String](
      "aws.message.writer.s3.bucketName",
      "Name of the S3 bucket where new message bodies are written")

  @Singleton
  @Provides
  def providesMessageWriterConfig(): MessageWriterConfig = {
    val snsConfig = SNSConfig(topicArn = writerTopicArn())
    val s3Config = S3Config(bucketName = writerBucketName())

    MessageWriterConfig(snsConfig = snsConfig, s3Config = s3Config)
  }
}
