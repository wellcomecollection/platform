package uk.ac.wellcome.platform.api.utils

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.BadRequestException

object ApiRequestUtils {

  def hostUrl(request: Request): String = {
    val scheme = request.headerMap.get("x-forwarded-proto") match {
      case Some(protocol) => protocol
      case _ => "http"
    }

    // Most users will access our API through a DNS record that comes through
    // https://api.wellcomecollection.org.  I can't find a way to detect
    // the DNS name through the request headers, so just hard-code it if we
    // detect the request came through ELB.
    //
    // Specifically, we look for a header that is added to all ELB requests:
    // http://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-request-tracing.html
    val hostHeader: String = request.headerMap.get("X-Amzn-Trace-Id") match {
      case Some(_) => "api.wellcomecollection.org"
      case None => {
        request.host match {
          case Some(host) => host
          case _ => throw new BadRequestException("Host header not set")
        }
      }
    }

    scheme + "://" + hostHeader
  }

}
