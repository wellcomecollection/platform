package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

object DynamoClientModule extends AbstractModule {

  @Singleton
  @Provides
  def providesDynamoClient(dynamoClientConfig: DynamoClientConfig): AmazonDynamoDB =
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
