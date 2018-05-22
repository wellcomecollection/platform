package uk.ac.wellcome.platform.matcher.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.platform.matcher.storage.MatcherDynamoConfig

object MatcherDynamoConfigModule extends TwitterModule {
  private val tableName =
    flag[String]("aws.dynamo.tableName", "", "Name of the DynamoDB table")

  private val tableIndex =
    flag[String]("aws.dynamo.tableIndex", "", "Name of the DynamoDB table index")

  @Singleton
  @Provides
  def providesDynamoConfig(): MatcherDynamoConfig =
    MatcherDynamoConfig(table = tableName(), index = tableIndex())
}
