package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig


trait DynamoConfigModule extends TwitterModule {
  val appNameTpl: String = "aws.dynamo.%s.streams.appName"
  val arnTpl: String = "aws.dynamo.%s.streams.arn"
  val tableTpl: String = "aws.dynamo.%s.tableName"

  def flags(tableName: String) = (
    flag[String](appNameTpl.format(tableName), "", "Name of the Kinesis app"),
    flag[String](arnTpl.format(tableName),"", "ARN of the DynamoDB stream"),
    flag[String](tableTpl.format(tableName),"","Name of the DynamoDB table")
  )
}

object PlatformDynamoConfigModule
    extends DynamoConfigModule {

  val (miroAppName, miroArn, miroTable) = flags("miroData")
  val (identAppName, identArn, identTable) = flags("identifiers")
  val (calmAppName, calmArn, calmTable) = flags("calmData")
  val (reindexAppName, reindexArn, reindexTable) = flags("reindexTracker")

  @Singleton
  @Provides
  def providesDynamoConfig(): Map[String, DynamoConfig] =
    Map(
      "calm" -> DynamoConfig(calmAppName(), calmArn(), calmTable()),
      "identifiers" -> DynamoConfig(identAppName(), identArn(), identTable()),
      "miro" -> DynamoConfig(miroAppName(), miroArn(), miroTable()),
      "reindex" -> DynamoConfig(reindexAppName(), reindexArn(), reindexTable())
    ).filterNot {
      case (_, v) => v.table.isEmpty
    }
}
