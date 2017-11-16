package uk.ac.wellcome.platform.sierra_dynamo.api

import com.fasterxml.jackson.databind.JsonNode


class SierraRetriever(apiUrl: String, oauthKey: String, oauthSecret: String) {
  def getObjects(str: String): JsonNode = ???

}
