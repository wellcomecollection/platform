package uk.ac.wellcome.platform.api

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Response, Status}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.versions.ApiVersions

import scala.collection.JavaConverters._

class ApiSwaggerTest extends FunSpec with Matchers with fixtures.Server {

  it("returns a valid JSON response for all api versions") {
    ApiVersions.values.toList.foreach { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")

      tree.at("/host").toString should be("\"test.host\"")
      tree.at("/schemes").toString should be("[\"http\"]")
      tree.at("/info/version").toString should be(s""""${version.toString}"""")
      tree.at("/basePath").toString should be(
        s""""/test/${version.toString}"""")
    }
  }

  it("includes the DisplayError model all api versions") {
    ApiVersions.values.toList.foreach { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/definitions/Error/type").toString should be("\"object\"")
    }
  }

  it("shows only the endpoints for the specified version") {
    ApiVersions.values.toList.foreach { version: ApiVersions.Value =>
      val tree = readTree(s"/test/${version.toString}/swagger.json")
      tree.at("/paths").isObject shouldBe true
      tree
        .at("/paths")
        .fieldNames
        .asScala
        .toList should contain only ("/works", "/works/{id}")
    }
  }

  it("shows the correct Work model for v1") {
    val tree = readTree(s"/test/${ApiVersions.v1.toString}/swagger.json")
    tree.at("/definitions/Work").isObject shouldBe true
    tree
      .at("/definitions/Work/properties/creators/type")
      .toString shouldBe "\"array\""
  }

  it("shows the correct Work model for v2") {
    val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
    tree.at("/definitions/Work").isObject shouldBe true
    tree
      .at("/definitions/Work/properties/contributors/type")
      .toString shouldBe "\"array\""
  }

  it("shows the correct Subject model for v2") {
    val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
    tree.at("/definitions/Subject").isObject shouldBe true
    tree
      .at("/definitions/Subject/properties/concepts/type")
      .toString shouldBe "\"array\""
    tree
      .at("/definitions/Subject/properties/concepts/items/$ref")
      .toString shouldBe "\"#/definitions/Concept\""
  }

  it("shows the correct Genre model for v2") {
    val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
    tree.at("/definitions/Genre").isObject shouldBe true
    tree
      .at("/definitions/Genre/properties/concepts/type")
      .toString shouldBe "\"array\""
    tree
      .at("/definitions/Genre/properties/concepts/items/$ref")
      .toString shouldBe "\"#/definitions/Concept\""
  }

  it("doesn't show DisplayWork in the definitions") {
    val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
    tree.at("/definitions").fieldNames.asScala.toList shouldNot contain(
      "DisplayWork")
  }

  it("shows Work as the items type in ResultList") {
    val tree = readTree(s"/test/${ApiVersions.v2.toString}/swagger.json")
    tree
      .at(s"/definitions/ResultList/properties/results/items/$$ref")
      .toString shouldBe "\"#/definitions/Work\""
  }

  // Because the Swagger should be the same on every request, we cache
  // requests for the Swagger to save the time cost of starting a new
  // server for every test/request.
  //
  var cachedResponses = Map[String, JsonNode]()

  def readTree(path: String): JsonNode = {
    cachedResponses.get(path) match {
      case Some(node) => node
      case None => {
        val newTree = readTreeFromServer(path)
        cachedResponses += (path -> newTree)
        newTree
      }
    }
  }

  def readTreeFromServer(path: String): JsonNode = {
    val flags = Map(
      "api.host" -> "test.host",
      "api.scheme" -> "http",
      "api.name" -> "test",
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
