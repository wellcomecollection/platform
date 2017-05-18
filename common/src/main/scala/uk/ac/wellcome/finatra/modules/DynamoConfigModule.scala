package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

trait DynamoConfigModule extends TwitterModule {
  val tableFlag: String

  private val applicationName = flag[String](
    s"aws.dynamo.$tableFlag.streams.appName", "", "Name of the Kinesis app")
  private val arn = flag[String](
    s"aws.dynamo.$tableFlag.streams.arn", "", "ARN of the DynamoDB stream")
  private val table = flag[String](
    s"aws.dynamo.$tableFlag.tableName", "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(applicationName(), arn(), table())
}

object CalmTableDynamoConfigModule extends DynamoConfigModule {
  val tableFlag = "calmData"
}

object MiroTableDynamoConfigModule extends DynamoConfigModule {
  val tableFlag = "miroData"
}

object IdentifierTableDynamoConfigModule extends DynamoConfigModule {
  val tableFlag = "identifiers"
}
