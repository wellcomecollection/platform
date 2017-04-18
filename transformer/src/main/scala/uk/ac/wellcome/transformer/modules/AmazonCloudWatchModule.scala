package uk.ac.wellcome.transformer.modules

import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

object AmazonCloudWatchModule extends TwitterModule {

  @Provides
  @Singleton
  def providesAmazonCloudWatch(dynamoConfig: DynamoConfig): AmazonCloudWatch = {
    AmazonCloudWatchClientBuilder
      .standard()
      .withRegion(dynamoConfig.region)
      .build()
  }
}
