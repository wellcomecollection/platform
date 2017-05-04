package uk.ac.wellcome.platform.api.utils

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.BadRequestException

trait ApiRequestUtils {

  val hostName: String

  def hostUrl(request: Request): String = {
    val scheme = request.headerMap.get("x-forwarded-proto") match {
      case Some(protocol) => protocol
      case _ => "http"
    }

    scheme + "://" + hostName 
  }

}
