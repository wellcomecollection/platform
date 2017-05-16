package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.AWSConfig

import com.amazonaws.services.dynamodbv2._

object DynamoClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)

  @Singleton
  @Provides
  def providesDynamoClient(awsConfig: AWSConfig): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder
      .standard()
      .withRegion(awsConfig.region)
      .build()

  @Singleton
  @Provides
  def providesDynamoAsyncClient(awsConfig: AWSConfig): AmazonDynamoDBAsync =
    AmazonDynamoDBAsyncClientBuilder
      .standard()
      .withRegion(awsConfig.region)
      .build()
}
