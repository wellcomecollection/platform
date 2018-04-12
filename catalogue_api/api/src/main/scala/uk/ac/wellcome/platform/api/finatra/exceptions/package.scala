package uk.ac.wellcome.platform.api.finatra

import com.twitter.finagle.http.Request

package object exceptions {
  // Shitty workaround to the fact that ExceptionMapper.toResponse method only allows
  // to pass in the request and the actual exception, so we need to
  // work out what the version was from the request
 def getVersion(request: Request, apiPrefix: String)= if(request.path.startsWith(s"$apiPrefix/v1")) "/v1"
 else throw new RuntimeException(s"No valid version in URL ${request.path}")
}
