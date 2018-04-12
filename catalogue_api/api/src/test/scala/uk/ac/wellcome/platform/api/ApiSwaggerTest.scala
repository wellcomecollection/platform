package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.IdentifiedWork

class ApiSwaggerTest extends FunSpec with Matchers with fixtures.Server {

  it("should return a valid JSON response") {
    val tree = readTree("/test/v1/swagger.json")

    tree.at("/host").toString should be("\"test.host\"")
    tree.at("/schemes").toString should be("[\"http\"]")
    tree.at("/info/version").toString should be("\"v1\"")
    tree.at("/basePath").toString should be("\"/test/v1\"")
  }

  it("should include the DisplayError model") {
    val tree = readTree("/test/v1/swagger.json")
    tree.at("/definitions/Error/type").toString should be("\"object\"")
  }

  def readTree(path: String): JsonNode = {
    val flags = Map(
      "api.host" -> "test.host",
      "api.scheme" -> "http",
      "api.name" -> "test"
    )

    implicit val jsonMapper = IdentifiedWork

    val response: Response = withServer(flags) { server =>
      server.httpGet(
        path = path,
        andExpect = Status.Ok
      )
    }

    val mapper = new ObjectMapper()

    noException should be thrownBy { mapper.readTree(response.contentString) }

    mapper.readTree(response.contentString)
  }
}
