package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.ApiVersions

import scala.collection.JavaConverters._

class ApiSwaggerTest extends FunSpec with Matchers with fixtures.Server {

  describe("all API versions") {
    it("returns a valid JSON response") {
      allResponses.foreach {
        case (version: ApiVersions.Value, response: JsonNode) =>
          response.at("/host").toString should be("\"test.host\"")
          response.at("/schemes").toString should be("[\"http\"]")
          response.at("/info/version").toString should be(
            s""""${version.toString}"""")
          response.at("/basePath").toString should be("")
      }
    }

    it("includes the DisplayError model") {
      allResponses.values.foreach { response: JsonNode =>
        response.at("/definitions/Error/type").toString should be("\"object\"")
      }
    }

    it("shows only the endpoints for the specified version") {
      allResponses.foreach {
        case (version: ApiVersions.Value, response: JsonNode) =>
          response.at("/paths").isObject shouldBe true
          response
            .at("/paths")
            .fieldNames
            .asScala
            .toList should contain only (s"/catalogue/${version.toString}/works", s"/catalogue/${version.toString}/works/{id}")
      }
    }

    it("doesn't show DisplayWork in the definitions") {
      allResponses.values.foreach { response: JsonNode =>
        response
          .at("/definitions")
          .fieldNames
          .asScala
          .toList shouldNot contain("DisplayWork")
      }
    }

    it("shows Work as the items type in ResultList") {
      allResponses.values.foreach { response: JsonNode =>
        response
          .at(s"/definitions/ResultList/properties/results/items/$$ref")
          .toString shouldBe "\"#/definitions/Work\""
      }
    }
  }

  describe("v1") {
    it("shows the correct Work model") {
      v1response.at("/definitions/Work").isObject shouldBe true
      v1response
        .at("/definitions/Work/properties/creators/type")
        .toString shouldBe "\"array\""
    }

    it("shows the includes request parameter model") {
      v1response.at("/definitions/Work").isObject shouldBe true
      val parameters = v1response
        .at("/paths")
        .findPath(s"/catalogue/${ApiVersions.v1.toString}/works")
        .at("/get/parameters")
        .asScala
        .map(_.findPath("name").asText())
      parameters should contain("includes")
      parameters should not contain "include"
    }

    it("doesn't include any filter parameters") {
      val parameters = v1response
        .at("/paths")
        .findPath(s"/catalogue/${ApiVersions.v1.toString}/works")
        .at("/get/parameters")
        .asScala
        .map(_.findPath("name").asText())

      parameters should not contain "items.locations.locationType"
      parameters should not contain "workType"
    }
  }

  describe("v2") {
    it("shows the correct Work model") {
      v2response.at("/definitions/Work").isObject shouldBe true
      v2response
        .at("/definitions/Work/properties/contributors/type")
        .toString shouldBe "\"array\""
    }

    it("shows the include request parameter model") {
      v2response.at("/definitions/Work").isObject shouldBe true
      val parameters = v2response
        .at("/paths")
        .findPath(s"/catalogue/${ApiVersions.v2.toString}/works")
        .at("/get/parameters")
        .asScala
        .map(_.findPath("name").asText())

      parameters should contain("include")
      parameters should not contain "includes"
    }

    it("shows the correct Subject model") {
      v2response.at("/definitions/Subject").isObject shouldBe true
      v2response
        .at("/definitions/Subject/properties/concepts/type")
        .toString shouldBe "\"array\""
      v2response
        .at("/definitions/Subject/properties/concepts/items/$ref")
        .toString shouldBe "\"#/definitions/Concept\""
    }

    it("shows the type of contributors") {
      v2response
        .at("/definitions/Work/properties/contributors/items/$ref")
        .toString shouldBe "\"#/definitions/Contributor\""
    }

    it("shows the correct Genre model") {
      v2response.at("/definitions/Genre").isObject shouldBe true
      v2response
        .at("/definitions/Genre/properties/concepts/type")
        .toString shouldBe "\"array\""
      v2response
        .at("/definitions/Genre/properties/concepts/items/$ref")
        .toString shouldBe "\"#/definitions/Concept\""
    }

    it("includes the filter query parameters") {
      val parameters = v2response
        .at("/paths")
        .findPath(s"/catalogue/${ApiVersions.v2.toString}/works")
        .at("/get/parameters")
        .asScala
        .map(_.findPath("name").asText())

      parameters should contain("items.locations.locationType")
      parameters should contain("workType")
    }
  }

  val v1response: JsonNode = readTree(
    s"/catalogue/${ApiVersions.v1.toString}/swagger.json")
  val v2response: JsonNode = readTree(
    s"/catalogue/${ApiVersions.v2.toString}/swagger.json")

  val allResponses = Map(
    ApiVersions.v1 -> v1response,
    ApiVersions.v2 -> v2response,
  )

  def readTree(path: String): JsonNode = {
    val flags = Map(
      "api.host" -> "test.host",
      "api.scheme" -> "http",
      "api.name" -> "catalogue",
      "es.index.v1" -> "v1",
      "es.index.v2" -> "v2"
    )

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
