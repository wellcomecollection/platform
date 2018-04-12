package uk.ac.wellcome.platform.api.finatra

import com.twitter.finagle.http.Request

package object exceptions {
 def getVersion(request: Request, apiPrefix: String)= if(request.path.startsWith(s"$apiPrefix/v1")) "/v1"
 else throw new RuntimeException(s"No valid version in URL ${request.path}")
}
