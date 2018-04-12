package uk.ac.wellcome.platform.api

object ContextHelper {
  def buildContextUri(apiScheme: String, apiHost: String, apiPrefix: String, version:String, apiContextSuffix: String) = s"$apiScheme://$apiHost$apiPrefix$version$apiContextSuffix"
}
