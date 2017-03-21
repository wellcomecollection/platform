package uk.ac.wellcome.platform.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

import com.amazonaws.services.sns._

/** Sent to request a change in the desired count of an ECS service. */
case class ECSServiceScheduleRequest(
  cluster: String, service: String, desired_count: Long)

object SNSClientModule extends TwitterModule {
  override val modules = Seq(SNSConfigModule)

  @Singleton
  @Provides
  def providesSNSClient(snsConfig: SNSConfig): AmazonSNS =
    AmazonSNSClientBuilder
      .standard()
      .withRegion(snsConfig.region)
      .build()
}
