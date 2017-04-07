package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.SNSConfig

import com.amazonaws.services.sns._

object SNSClientModule extends TwitterModule {

  @Singleton
  @Provides
  def providesSNSClient(snsConfig: SNSConfig): AmazonSNS =
    AmazonSNSClientBuilder
      .standard()
      .withRegion(snsConfig.region)
      .build()
}
