package uk.ac.wellcome.platform.api

import uk.ac.wellcome.models.ApiVersions

object ContextHelper {
  def buildContextUri(apiScheme: String,
                      apiHost: String,
                      apiPrefix: String,
                      version: ApiVersions.Value,
                      apiContextSuffix: String) =
    s"$apiScheme://$apiHost$apiPrefix/$version$apiContextSuffix"
}
