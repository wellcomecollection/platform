package uk.ac.wellcome.platform.sierra_dynamo.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.annotation.tailrec
import scala.util.parsing.json.JSONArray
import scalaj.http.Http
import scala.collection.JavaConversions._


class SierraRetriever(apiUrl: String, oauthKey: String, oauthSecret: String) {
  val mapper = new ObjectMapper()

  var token: String = refreshToken()

  def getObjects(str: String, params: Map[String,String] = Map.empty): Stream[JsonNode] = {
    val response = Http(s"$apiUrl/$str").params(params)
      .header("Authorization", s"Bearer $token")
      .header("Accept", "application/json").asString

    response.code match {
      case 200 =>
        val list = mapper.readTree(response.body).path("entries").elements()
        val items = list.toList
        val lastId = items.last.path("id").asInt()

        items.toStream append getObjects(str, params + ("id" -> s"[${lastId + 1},]"))
      case 404 => Stream.empty
    }
  }

  private def refreshToken() = {
    val tokenResponse = Http(s"$apiUrl/token").postForm.auth(oauthKey, oauthSecret).asString
    mapper.readTree(tokenResponse.body).path("access_token").asText()
  }

}
