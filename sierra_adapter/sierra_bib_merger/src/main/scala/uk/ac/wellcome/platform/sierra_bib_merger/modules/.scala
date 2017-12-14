package uk.ac.wellcome.platform.sierra_bib_merger.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraDynamoConfigModule extends DynamoConfigModule {
  val sierraBibMergerTable = flags("sierraBibMerger")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(sierraBibMergerTable())
}
