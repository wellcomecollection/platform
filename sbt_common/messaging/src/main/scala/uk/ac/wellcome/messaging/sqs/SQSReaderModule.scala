package uk.ac.wellcome.messaging.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule

object SQSReaderModule extends TwitterModule {

  @Singleton
  @Provides
  def providesSQSReader(sqsClient: AmazonSQS, sqsConfig: SQSConfig) =
    new SQSReader(sqsClient, sqsConfig)
}
