package uk.ac.wellcome.messaging.message

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.storage.s3.S3Config

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

  @Singleton
  @Provides
  def providesMessageConfig(): MessageConfig = {
    val snsConfig = SNSConfig(topicArn = topicArn())
    val s3Config = S3Config(bucketName = bucketName())
    MessageConfig(snsConfig = snsConfig, s3Config = s3Config)
  }
}
