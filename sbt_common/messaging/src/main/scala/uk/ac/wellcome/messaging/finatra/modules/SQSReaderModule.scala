package uk.ac.wellcome.messaging.finatra.modules

import com.amazonaws.services.sqs.AmazonSQS
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.sqs.SQSReader

object SQSReaderModule extends TwitterModule {

  @Singleton
  @Provides
  def providesSQSReader(sqsClient: AmazonSQS, sqsConfig: SQSConfig) =
    new SQSReader(sqsClient, sqsConfig)
}
