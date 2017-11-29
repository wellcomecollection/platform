package uk.ac.wellcome.platform.sierra_to_dynamo.modules

import com.google.inject.Provides
import uk.ac.wellcome.finatra.modules.DynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig
import javax.inject.Singleton

object SierraDynamoConfigModule extends DynamoConfigModule{
  val (sierraToDynamoAppName, sierraToDynamoAppArn, sierraToDynamoAppTable) = flags("sierraToDynamo")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig = DynamoConfig(sierraToDynamoAppName(), sierraToDynamoAppArn(), sierraToDynamoAppTable())
}
