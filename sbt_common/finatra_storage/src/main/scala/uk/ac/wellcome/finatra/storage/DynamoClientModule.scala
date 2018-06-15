package uk.ac.wellcome.finatra.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

object DynamoClientModule extends TwitterModule {
  private val dynamoDbEndpoint = flag[String](
    "aws.dynamoDb.endpoint",
    "",
    "Endpoint of AWS DynamoDB. if not present it will use the region")
  private val accessKey =
    flag[String]("aws.dynamoDb.accessKey", "", "AccessKey to access DynamoDB")
  private val secretKey =
    flag[String]("aws.dynamoDb.secretKey", "", "SecretKey to access DynamoDB")

  private val region = flag[String](
    name = "aws.dynamoDb.region",
    default = "eu-west-1",
    help = "AWS region for dynamoDb")

  @Singleton
  @Provides
  def providesDynamoClient(): AmazonDynamoDB =
    DynamoClientFactory.create(
      region = region(),
      endpoint = dynamoDbEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )
}
