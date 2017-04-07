package uk.ac.wellcome.finatra.modules

import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.sqs.SQSReader

import scala.concurrent.duration._

object SQSReaderModule extends TwitterModule {
    val waitime = flag[String]("aws.sqs.wait.seconds", "10", "SQS read wait time in seconds")
    val maxMessages = flag("sqs.maxMessages", 1, "Max SQS messages")

  @Singleton
  @Provides
  def providesSQSReader(sqsClient:AmazonSQS, sqsConfig: SQSConfig) =
    new SQSReader(sqsClient, sqsConfig, waitime().toInt seconds, maxMessages())
}
