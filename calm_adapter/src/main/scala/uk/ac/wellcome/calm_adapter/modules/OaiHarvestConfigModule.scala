package uk.ac.wellcome.platform.calm_adapter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

case class OaiHarvestConfig(oaiUrl: String)

object OaiHarvestConfigModule extends TwitterModule {

  private val oaiUrl = flag(
    name = "oaiUrl",
    default = "http://archives.wellcomelibrary.org/oai/OAI.aspx",
    help = "Base URL for OAI request"
  )

  @Singleton
  @Provides
  def providesOaiHarvestConfig(): OaiHarvestConfig = OaiHarvestConfig(oaiUrl())

}
