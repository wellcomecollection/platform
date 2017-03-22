package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SQSConfig

import com.amazonaws.services.sqs._

object SQSClientModule extends TwitterModule {
  override val modules = Seq(SQSConfigModule)

  @Singleton
  @Provides
  def providesSQSClient(sqsConfig: SQSConfig): AmazonSQS =
    AmazonSQSClientBuilder
      .standard()
      .withRegion(sqsConfig.region)
      .build()

}
