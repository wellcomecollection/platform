package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

trait DynamoConfigModule extends TwitterModule {
  val arnTpl: String = "aws.dynamo.%s.streams.arn"
  val tableTpl: String = "aws.dynamo.%s.tableName"

  def flags(tableName: String) = (
    flag[String](arnTpl.format(tableName), "", "ARN of the DynamoDB stream"),
    flag[String](tableTpl.format(tableName), "", "Name of the DynamoDB table")
  )
}

object PlatformDynamoConfigModule extends DynamoConfigModule {

  val (miroArn, miroTable) = flags("miroData")
  val (identArn, identTable) = flags("identifiers")
  val (calmArn, calmTable) = flags("calmData")
  val (reindexArn, reindexTable) = flags("reindexTracker")

  @Singleton
  @Provides
  def providesDynamoConfig(): Map[String, DynamoConfig] =
    Map(
      "calm" -> DynamoConfig(calmArn(), calmTable()),
      "identifiers" -> DynamoConfig(identArn(), identTable()),
      "miro" -> DynamoConfig(miroArn(), miroTable()),
      "reindex" -> DynamoConfig(reindexArn(), reindexTable())
    ).filterNot {
      case (_, v) => v.table.isEmpty
    }
}
