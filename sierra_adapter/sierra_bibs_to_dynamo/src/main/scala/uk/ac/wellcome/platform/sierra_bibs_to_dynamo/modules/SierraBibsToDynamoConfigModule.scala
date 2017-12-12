package uk.ac.wellcome.platform.sierra_bibs_to_dynamo.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraBibsToDynamoConfigModule extends DynamoConfigModule {
  val sierraToDynamoAppTable = flags("sierraBibsToDynamo")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(sierraToDynamoAppTable())
}
