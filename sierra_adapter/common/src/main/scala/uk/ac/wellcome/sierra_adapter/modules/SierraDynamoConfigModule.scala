package uk.ac.wellcome.sierra_adapter.modules

import com.google.inject.Provides
import javax.inject.Singleton

import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig

object SierraDynamoConfigModule extends DynamoConfigModule {
  val sierraTable = flags("sierra")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(sierraTable())
}
