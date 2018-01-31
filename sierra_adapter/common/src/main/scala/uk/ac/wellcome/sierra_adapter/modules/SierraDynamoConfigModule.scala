package uk.ac.wellcome.sierra_adapter.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraDynamoConfigModule extends DynamoConfigModule {

  val sierraToDynamo = flags("sierraToDynamo")
  val merger = flags("merger")
  val dynamoTable = flags("dynamoTable")

  @Singleton
  @Provides
  def providesDynamoConfig(): Map[String, DynamoConfig] =
    Map(
      "sierraToDynamo" -> DynamoConfig(sierraToDynamo()),
      "merger" -> DynamoConfig(merger())
    ).filterNot {
      case (_, v) => v.table.isEmpty
    }

  @Singleton
  @Provides
  def providesSingleDynamoConfig(): DynamoConfig =
    DynamoConfig(dynamoTable())
}
