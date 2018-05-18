package uk.ac.wellcome.finatra.storage

import javax.inject.Singleton

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.AWSConfigModule
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

object DynamoClientModule extends TwitterModule {
  override val modules = Seq(AWSConfigModule)
  private val dynamoDbEndpoint = flag[String](
    "aws.dynamoDb.endpoint",
    "",
    "Endpoint of AWS DynamoDB. if not present it will use the region")
  private val accessKey =
    flag[String]("aws.dynamoDb.accessKey", "", "AccessKey to access DynamoDB")
  private val secretKey =
    flag[String]("aws.dynamoDb.secretKey", "", "SecretKey to access DynamoDB")

  @Singleton
  @Provides
  def providesDynamoClient(awsConfig: AWSConfig): AmazonDynamoDB =
    DynamoClientFactory.create(
      region = awsConfig.region,
      endpoint = dynamoDbEndpoint(),
      accessKey = accessKey(),
      secretKey = secretKey()
    )
}
