package uk.ac.wellcome.platform.api.utils

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.BadRequestException

object ApiRequestUtils {

  def hostUrl(request: Request): String = {
    val scheme = request.headerMap.get("x-forwarded-proto") match {
      case Some(protocol) => protocol
      case _ => "http"
    }

    val hostHeader = request.host match {
      case Some(host) => host
      case _ => throw new BadRequestException("Host header not set")
    }

    scheme + "://" + hostHeader
  }

}
