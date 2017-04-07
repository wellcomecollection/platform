package uk.ac.wellcome.platform.idminter.modules

import javax.inject.Singleton

import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig

object AmazonDynamoDBModule extends TwitterModule {

  @Singleton
  @Provides
  def providesAmazonDynamoDB(dynamoConfig: DynamoConfig): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder.standard().withRegion(dynamoConfig.region).build()

}
