package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{ApiVersions, IdentifiedWork}
import org.scalacheck.ScalacheckShapeless._
import scala.collection.JavaConversions._

class ApiSwaggerTest extends FunSpec with Matchers with fixtures.Server with PropertyChecks {

  it("should return a valid JSON response for all api versions") {
    forAll {version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")

      tree.at("/host").toString should be("\"test.host\"")
      tree.at("/schemes").toString should be("[\"http\"]")
      tree.at("/info/version").toString should be(s""""${version.toString}"""")
      tree.at("/basePath").toString should be(s""""/test/${version.toString}"""")
    }
  }

  it("should include the DisplayError model all api versions") {
    forAll { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/definitions/Error/type").toString should be("\"object\"")
    }
  }

  it("should show only the endpoints for the specified version") {
    forAll { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/paths").isObject shouldBe true
      tree.at("/paths").fieldNames.toList should contain only ("/works", "/works/{id}")
    }
  }

  it("should show the correct Work model for v1") {
      val tree = readTree(s"/test/${ApiVersions.v1.toString}/swagger.json")
      tree.at("/definitions/Work").isObject shouldBe true
      tree.at("/definitions/Work/properties/creators/type") shouldBe "array"
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
