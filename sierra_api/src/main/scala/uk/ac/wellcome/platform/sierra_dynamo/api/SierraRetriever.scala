package uk.ac.wellcome.platform.sierra_dynamo.api

import io.circe._
import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.slf4j.LoggerFactory

import scala.util.Try
import scalaj.http.{Http, HttpResponse}

class SierraRetriever(apiUrl: String, oauthKey: String, oauthSecret: String) {
  val logger = LoggerFactory.getLogger(this.getClass)

  var token: String = refreshToken()
  val maxRetries = 1

  def getObjects(resourceType: String,
                 params: Map[String, String] = Map.empty,
                 refreshTokenAttempt: Int = 0): Stream[Json] = {


    val response = Http(s"$apiUrl/$resourceType")
      .params(params)
      .header("Authorization", s"Bearer $token")
      .header("Accept", "application/json")
      .asString

    response.code match {
      case 200 =>
        buildNodeStream(resourceType, params, response)
      case 404 => Stream.empty
      case 401 =>
        refreshTokenAndRetry(resourceType, params, refreshTokenAttempt)
      case errorCode =>
        logger.error(s"Received error response: $errorCode ${response.body}")
        throw new RuntimeException(response.body)
    }
  }

  private def buildNodeStream(str: String,
                              params: Map[String, String],
                              response: HttpResponse[String]) = {
    val json = parse(response.body).right.getOrElse(throw new RuntimeException("response was not json"))

    val list = root.entries.each.json.getAll(json)
    val lastId = root.id.string.getOption(list.last).getOrElse(throw new RuntimeException("id not found in item")).toInt

    list.toStream append getObjects(str,
                                     params + ("id" -> s"[${lastId + 1},]"))
  }

  private def refreshTokenAndRetry(str: String,
                                   params: Map[String, String],
                                   refreshTokenAttempt: Int) = {
    refreshTokenAttempt match {
      case x: Int if x < maxRetries =>
        token = refreshToken()
        getObjects(str, params, 1)
      case _ => throw new RuntimeException("Exceeded number of auth attempts")
    }
  }

  private def refreshToken() = {
    val tokenResponse =
      Http(s"$apiUrl/token").postForm.auth(oauthKey, oauthSecret).asString
    val json = parse(tokenResponse.body).right.getOrElse(throw new RuntimeException("response was not json"))
    root.access_token.string.getOption(json).get
  }

}
