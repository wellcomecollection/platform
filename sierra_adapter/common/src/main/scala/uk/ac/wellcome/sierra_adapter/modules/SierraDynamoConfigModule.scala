package uk.ac.wellcome.platform.sierra_adapter.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraDynamoConfigModule extends DynamoConfigModule {
  val tableName = flags("merger")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(tableName())
}
