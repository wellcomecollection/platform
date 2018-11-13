package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.idminter.config.models.IdentifiersTableConfig

object IdentifiersTableConfigModule extends TwitterModule {
  val database = flag[String](
    "aws.rds.identifiers.database",
    "",
    "Name of the identifiers database")
  val tableName = flag[String](
    "aws.rds.identifiers.table",
    "",
    "Name of the identifiers table")

  @Provides
  def providesIdentifiersTableConfig(): IdentifiersTableConfig =
    IdentifiersTableConfig(
      database = database(),
      tableName = tableName()
    )
}
