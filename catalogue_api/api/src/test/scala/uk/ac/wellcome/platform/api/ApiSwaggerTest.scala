package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import uk.ac.wellcome.models.IdentifiedWork

class ApiSwaggerTest extends FunSpec with FeatureTestMixin {

  implicit val jsonMapper = IdentifiedWork
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "api.host" -> "test.host",
        "api.scheme" -> "http",
        "api.version" -> "v99",
        "api.name" -> "test"
      )
    )

  it("should return a valid JSON response") {
    val tree = readTree("/test/v99/swagger.json")

    tree.at("/host").toString should be("\"test.host\"")
    tree.at("/schemes").toString should be("[\"http\"]")
    tree.at("/info/version").toString should be("\"v99\"")
    tree.at("/basePath").toString should be("\"/test/v99\"")
  }

  it("should include the DisplayError model") {
    val tree = readTree("/test/v99/swagger.json")
    tree.at("/definitions/Error/type").toString should be("\"object\"")
  }

  def readTree(path: String): JsonNode = {
    val response: Response = server.httpGet(
      path = path,
      andExpect = Status.Ok
    )

    val mapper = new ObjectMapper()

    noException should be thrownBy { mapper.readTree(response.contentString) }

    mapper.readTree(response.contentString)
  }
}
