package uk.ac.wellcome.storage.dynamo

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  private val tableName =
    flag[String]("aws.dynamo.tableName", "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(table = tableName())
}
