package uk.ac.wellcome.finatra.storage

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.storage.dynamo.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  private val tableName =
    flag[String]("aws.dynamo.tableName", "Name of the DynamoDB table")
  private val tableIndex =
    flag[String]("aws.dynamo.tableIndex", "", "Name of the DynamoDB index")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(
      table = tableName(),
      maybeIndex = if (tableIndex().isEmpty) None else Some(tableIndex())
    )
}
