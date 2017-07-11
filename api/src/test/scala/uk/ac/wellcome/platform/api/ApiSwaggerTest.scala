package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.FunSpec
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import uk.ac.wellcome.utils.JsonUtil.mapper

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
    val response: Response = server.httpGet(
      path = "/test/v99/swagger.json",
      andExpect = Status.Ok
    )

    val mapper = new ObjectMapper()

    noException should be thrownBy { mapper.readTree(response.contentString) }

    val tree = mapper.readTree(response.contentString)

    tree.at("/host").toString should be("\"test.host\"")
    tree.at("/schemes").toString should be("[\"http\"]")
    tree.at("/info/version").toString should be("\"v99\"")
    tree.at("/basePath").toString should be("\"/test/v99\"")
  }
}
