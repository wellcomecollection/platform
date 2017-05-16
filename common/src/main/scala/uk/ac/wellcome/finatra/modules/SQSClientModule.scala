package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

import com.amazonaws.services.sqs._

object SQSClientModule extends TwitterModule {
  override val modules = Seq(SQSConfigModule)

  @Singleton
  @Provides
  def providesSQSClient(awsConfig: AWSConfig): AmazonSQS =
    AmazonSQSClientBuilder
      .standard()
      .withRegion(awsConfig.region)
      .build()

}
