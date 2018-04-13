package uk.ac.wellcome.platform.api.finatra

import com.twitter.finagle.http.Request
import uk.ac.wellcome.models.ApiVersions

package object exceptions {
  // Shitty workaround to the fact that ExceptionMapper.toResponse method only allows
  // to pass in the request and the actual exception, so we need to
  // work out what the version was from the request
  def getVersion(request: Request, apiPrefix: String): ApiVersions.Value = {
    val maybeVersion = ApiVersions.values.find { apiVersion => request.path.startsWith(s"$apiPrefix/${apiVersion.toString}") }
    maybeVersion.getOrElse(throw new RuntimeException(s"No valid version in URL ${request.path}"))
  }
}
