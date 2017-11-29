package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

trait DynamoConfigModule extends TwitterModule {
  val tableTpl: String = "aws.dynamo.%s.tableName"

  def flags(tableName: String) =
    flag[String](tableTpl.format(tableName), "", "Name of the DynamoDB table")
}

object PlatformDynamoConfigModule extends DynamoConfigModule {

  val miroTable = flags("miroData")
  val identTable = flags("identifiers")
  val calmTable = flags("calmData")
  val reindexTable = flags("reindexTracker")

  @Singleton
  @Provides
  def providesDynamoConfig(): Map[String, DynamoConfig] =
    Map(
      "calm" -> DynamoConfig(calmTable()),
      "identifiers" -> DynamoConfig(identTable()),
      "miro" -> DynamoConfig(miroTable()),
      "reindex" -> DynamoConfig(reindexTable())
    ).filterNot {
      case (_, v) => v.table.isEmpty
    }
}
