package uk.ac.wellcome.utils

import java.net.URLEncoder


object UrlUtil {
  def urlEncode(s: String) =
    URLEncoder.encode(s, "UTF-8")

  def buildUri(
    path: String,
    params: Map[String, String] = Map.empty
  ): String = path + params.map {
      case (k,v) => s"${urlEncode(k)}=${urlEncode(v)}"
    }.mkString("?", "&", "")
}
