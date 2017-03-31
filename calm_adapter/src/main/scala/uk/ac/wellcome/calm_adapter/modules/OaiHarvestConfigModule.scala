package uk.ac.wellcome.platform.calm_adapter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule

import uk.ac.wellcome.platform.calm_adapter.models.OaiHarvestConfig

object OaiHarvestConfigModule extends TwitterModule {

  private val oaiUrl = flag(
    name = "oaiUrl",
    default = "http://archives.wellcomelibrary.org/oai/OAI.aspx",
    help = "Base URL for OAI request"
  )

  private val oaiDaysToFetch = flag(
    name = "oaiDaysToFetch",
    default = 3L,
    help = ("How many days of records to fetch from the OAI. " ++
      "Set to -1 to fetch all records.")
  )

  @Singleton
  @Provides
  def providesOaiHarvestConfig(): OaiHarvestConfig =
    OaiHarvestConfig(oaiUrl(), oaiDaysToFetch())

}
