package uk.ac.wellcome.platform.sierra_items_to_dynamo.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraDynamoConfigModule extends DynamoConfigModule {
  val sierraItemsToDynamoAppTable = flags("sierraItemsToDynamo")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(sierraItemsToDynamoAppTable())
}
