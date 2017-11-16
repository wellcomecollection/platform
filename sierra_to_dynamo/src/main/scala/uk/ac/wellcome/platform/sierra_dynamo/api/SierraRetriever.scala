package uk.ac.wellcome.platform.sierra_dynamo.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scalaj.http.Http


class SierraRetriever(apiUrl: String, oauthKey: String, oauthSecret: String) {
  val mapper = new ObjectMapper()

  var token: String = refreshToken()

  def getObjects(str: String): JsonNode = {
    println(token)
    val response = Http(s"$apiUrl/$str").header("Authorization", s"Bearer $token").header("Accept", "application/json").asString

    mapper.readTree(response.body).path("entries")
  }

  private def refreshToken() = {
    val tokenResponse = Http(s"$apiUrl/token").postForm.auth(oauthKey, oauthSecret).asString
    mapper.readTree(tokenResponse.body).path("access_token").asText()
  }

}
