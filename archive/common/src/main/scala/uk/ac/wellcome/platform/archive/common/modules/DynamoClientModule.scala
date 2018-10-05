package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

object DynamoClientModule extends AbstractModule {
  import EnrichConfig._

  @Singleton
  @Provides
  def providesDynamoClientConfig(config: Config) = {
    val key = config
      .get[String]("aws.dynamo.key")

    val secret = config
      .get[String]("aws.dynamo.secret")

    val endpoint = config
      .get[String]("aws.dynamo.endpoint")

    val region = config
      .getOrElse[String]("aws.dynamo.region")("eu-west-1")

    DynamoClientConfig(key, secret, endpoint, region)
  }

  @Singleton
  @Provides
  def providesDynamoClient(
    dynamoClientConfig: DynamoClientConfig): AmazonDynamoDB =
    DynamoClientFactory.create(
      region = dynamoClientConfig.region,
      endpoint = dynamoClientConfig.endpoint.getOrElse(""),
      accessKey = dynamoClientConfig.accessKey.getOrElse(""),
      secretKey = dynamoClientConfig.secretKey.getOrElse(""),
    )
}

case class DynamoClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)
