package uk.ac.wellcome.messaging.sqs

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.sqs.SQSConfig

import scala.concurrent.duration._

object SQSConfigModule extends TwitterModule {
  private val queueUrl =
    flag[String]("aws.sqs.queue.url", "", "URL of the SQS Queue")
  val waitTime = flag(
    "aws.sqs.waitTime",
    20,
    "Time to wait (in seconds) for a message to arrive on the queue before returning")
  val maxMessages =
    flag("aws.sqs.maxMessages", 10, "Maximum number of SQS messages to return")

  @Singleton
  @Provides
  def providesSQSConfig(): SQSConfig =
    SQSConfig(queueUrl(), waitTime() seconds, maxMessages())
}
