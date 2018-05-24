package uk.ac.wellcome.finatra.messaging

import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}

object SQSReaderModule extends TwitterModule {

  @Singleton
  @Provides
  def providesSQSReader(sqsClient: AmazonSQS, sqsConfig: SQSConfig) =
    new SQSReader(sqsClient, sqsConfig)
}
