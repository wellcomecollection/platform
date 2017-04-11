package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SQSConfig

import scala.concurrent.duration._

object SQSConfigModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")
  private val queueUrl =
    flag[String]("aws.sqs.queue.url", "", "URL of the SQS Queue")
  val waitTime = flag(
    "aws.sqs.waitTime",
    20,
    "Time to wait (in seconds) for a message to arrive on the queue before returning")
  val maxMessages =
    flag("aws.sqs.maxMessages", 1, "Maximum number of SQS messages to return")

  @Singleton
  @Provides
  def providesSQSConfig(): SQSConfig =
    SQSConfig(region(), queueUrl(), waitTime() seconds, maxMessages())
}
