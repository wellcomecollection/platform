package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  private val applicationName = flag[String]("aws.dynamo.streams.appName",
                                             "dynamodb-streams-app",
                                             "Name of the Kinesis app")
  private val arn =
    flag[String]("aws.dynamo.streams.arn", "", "ARN of the DynamoDB stream")
  private val table =
    flag[String]("aws.dynamo.tableName", "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(applicationName(), arn(), table())
}
