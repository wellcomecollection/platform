package uk.ac.wellcome.finatra.storage

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.storage.dynamo.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  private val tableName =
    flag[String]("aws.dynamo.tableName", "Name of the DynamoDB table")
  private val tableIndex =
    flag[String]("aws.dynamo.tableIndex", "", "Name of the DynamoDB index")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig = {
    DynamoConfig(
      table = tableName(),
      index = if (tableIndex().isEmpty) None else Some(tableIndex()))
  }
}
