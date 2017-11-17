package uk.ac.wellcome.platform.sierra_dynamo.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.JavaConversions._
import scalaj.http.{Http, HttpResponse}

class SierraRetriever(apiUrl: String, oauthKey: String, oauthSecret: String) {
  val mapper = new ObjectMapper()

  var token: String = refreshToken()
  val maxRetries = 1

  def getObjects(str: String,
                 params: Map[String, String] = Map.empty,
                 refreshTokenAttempt: Int = 0): Stream[JsonNode] = {
    val response = Http(s"$apiUrl/$str")
      .params(params)
      .header("Authorization", s"Bearer $token")
      .header("Accept", "application/json")
      .asString

    response.code match {
      case 200 =>
        buildNodeStream(str, params, response)
      case 404 => Stream.empty
      case 401 =>
        refreshTokenAndRetry(str, params, refreshTokenAttempt)
    }
  }

  private def buildNodeStream(str: String,
                              params: Map[String, String],
                              response: HttpResponse[String]) = {
    val list = mapper.readTree(response.body).path("entries").elements()
    val items = list.toList
    val lastId = items.last.path("id").asInt()

    items.toStream append getObjects(str,
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
    mapper.readTree(tokenResponse.body).path("access_token").asText()
  }

}
