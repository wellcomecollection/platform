package uk.ac.wellcome.platform.idminter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.PlatformDynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig

object IdMinterDynamoConfigModule extends TwitterModule {
  override val modules = Seq(PlatformDynamoConfigModule)

  @Singleton
  @Provides
  def providesDynamoConfig(
    platformDynamoConfig: Map[String, DynamoConfig]): DynamoConfig =
    platformDynamoConfig.getOrElse(
      "identifiers",
      throw new RuntimeException("Identifiers dynamo config missing"))

}
