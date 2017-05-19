package uk.ac.wellcome.calm_adapter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.modules.PlatformDynamoConfigModule
import uk.ac.wellcome.models.aws.DynamoConfig

object CalmAdapterDynamoConfigModule extends TwitterModule {
  override val modules = Seq(PlatformDynamoConfigModule)

  @Singleton
  @Provides
  def providesDynamoConfig(
    platformDynamoConfig: Map[String, DynamoConfig]): DynamoConfig =
    platformDynamoConfig.getOrElse(
      "calm",
      throw new RuntimeException("Calm dynamo config missing"))

}
