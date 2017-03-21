package uk.ac.wellcome.platform.ingestor.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

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
