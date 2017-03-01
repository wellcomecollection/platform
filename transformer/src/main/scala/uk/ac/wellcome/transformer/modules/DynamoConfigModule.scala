package uk.ac.wellcome.platform.transformer.modules

import javax.inject.Singleton

import com.google.inject.Provides

import com.twitter.inject.TwitterModule


case class DynamoConfig(region: String)

object DynamoConfigModule extends TwitterModule {
  private val region = flag[String]("aws.region", "eu-west-1", "AWS region")

  @Singleton
  @Provides
  def providesDynamoConfig(): DynamoConfig = DynamoConfig(region())
}
