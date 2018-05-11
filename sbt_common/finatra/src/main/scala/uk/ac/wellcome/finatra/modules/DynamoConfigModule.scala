package uk.ac.wellcome.finatra.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.storage.dynamo.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  private val tableName =
    flag[String]("aws.dynamo.tableName", "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(table = tableName())
}
