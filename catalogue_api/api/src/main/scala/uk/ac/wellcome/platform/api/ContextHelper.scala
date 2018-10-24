package uk.ac.wellcome.platform.api

import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.platform.api.models.ApiConfig

object ContextHelper {
  def buildContextUri(apiConfig: ApiConfig,
                      version: ApiVersions.Value = ApiVersions.default) =
    s"${apiConfig.scheme}://${apiConfig.host}${apiConfig.pathPrefix}/$version${apiConfig.contextSuffix}"
}
