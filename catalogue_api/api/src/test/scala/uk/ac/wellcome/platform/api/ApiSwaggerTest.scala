package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.IdentifiedWork
import org.scalacheck.ScalacheckShapeless._
import uk.ac.wellcome.versions.ApiVersions

import scala.collection.JavaConversions._

class ApiSwaggerTest extends FunSpec with Matchers with fixtures.Server with PropertyChecks {

  it("returns a valid JSON response for all api versions") {
    forAll {version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")

      tree.at("/host").toString should be("\"test.host\"")
      tree.at("/schemes").toString should be("[\"http\"]")
      tree.at("/info/version").toString should be(s""""${version.toString}"""")
      tree.at("/basePath").toString should be(s""""/test/${version.toString}"""")
    }
  }

  it("includes the DisplayError model all api versions") {
    forAll { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/definitions/Error/type").toString should be("\"object\"")
    }
  }

  it("shows only the endpoints for the specified version") {
    forAll { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/paths").isObject shouldBe true
      tree.at("/paths").fieldNames.toList should contain only ("/works", "/works/{id}")
    }
  }

  it("shows the correct Work model for v1") {
      val tree = readTree(s"/test/${ApiVersions.v1.toString}/swagger.json")
      tree.at("/definitions/Work").isObject shouldBe true
      tree.at("/definitions/Work/properties/creators/type").toString shouldBe "\"array\""
  }

  it("shows the correct Work model for v2") {
      val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
      tree.at("/definitions/Work").isObject shouldBe true
      tree.at("/definitions/Work/properties/contributors/type").toString shouldBe "\"array\""
  }

  it("doesn't show DisplayWork in the definitions") {
      val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
      tree.at("/definitions").fieldNames.toList shouldNot contain ("DisplayWork")
  }

  it("shows Work as the items type in ResultList") {
      val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
      tree.at(s"/definitions/ResultList/properties/results/items/$$ref").toString shouldBe "\"#/definitions/Work\""
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
