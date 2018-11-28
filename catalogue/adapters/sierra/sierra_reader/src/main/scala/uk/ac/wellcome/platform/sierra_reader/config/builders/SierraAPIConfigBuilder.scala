package uk.ac.wellcome.platform.sierra_reader.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.platform.sierra_reader.config.models.SierraAPIConfig

object SierraAPIConfigBuilder {
  def buildSierraConfig(config: Config): SierraAPIConfig =
    SierraAPIConfig(
      apiURL = config.required[String]("sierra.apiURL"),
      oauthKey = config.required[String]("sierra.oauthKey"),
      oauthSec = config.required[String]("sierra.oauthSecret")
    )
}
