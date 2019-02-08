package uk.ac.wellcome.platform.idminter.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.platform.idminter.config.models.IdentifiersTableConfig
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object IdentifiersTableBuilder {
  def buildConfig(config: Config): IdentifiersTableConfig = {
    val database = config.required[String]("aws.rds.identifiers.database")
    val tableName = config.required[String]("aws.rds.identifiers.table")

    IdentifiersTableConfig(
      database = database,
      tableName = tableName
    )
  }
}
