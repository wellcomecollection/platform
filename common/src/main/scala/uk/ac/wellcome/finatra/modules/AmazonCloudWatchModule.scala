package uk.ac.wellcome.finatra.modules

import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

object AmazonCloudWatchModule extends TwitterModule {

  @Provides
  @Singleton
  def providesAmazonCloudWatch(awsConfig: AWSConfig): AmazonCloudWatch = {
    AmazonCloudWatchClientBuilder
      .standard()
      .withRegion(awsConfig.region)
      .build()
  }
}
