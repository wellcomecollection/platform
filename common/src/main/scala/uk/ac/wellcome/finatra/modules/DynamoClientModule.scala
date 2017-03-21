package uk.ac.wellcome.platform.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

import com.amazonaws.services.dynamodbv2._

object DynamoClientModule extends TwitterModule {
  override val modules = Seq(DynamoConfigModule)

  @Singleton
  @Provides
  def providesDynamoClient(dynamoConfig: DynamoConfig): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder
      .standard()
      .withRegion(dynamoConfig.region)
      .build()

  @Singleton
  @Provides
  def providesDynamoAsyncClient(dynamoConfig: DynamoConfig): AmazonDynamoDBAsync =
    AmazonDynamoDBAsyncClientBuilder
      .standard()
      .withRegion(dynamoConfig.region)
      .build()
}
