package uk.ac.wellcome.platform.archive.progress_http.services
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSpec, Matchers}
import io.circe.Json
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.optics.JsonPath.root
import io.circe.parser._

class SwaggerDocServiceTest extends FunSpec  with Matchers with ScalatestRouteTest {
  it("exposes a swagger endpoint") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      contentType shouldBe ContentTypes.`application/json`
      val json = entityAs[Json]
      root.host.string.getOption(json) shouldBe Some("localhost:9001")
      root.basePath.string.getOption(json) shouldBe Some("/")
      root.info.version.string.getOption(json) shouldBe Some("v1")
    }
  }

  it("documents the get endpoint for progress") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      val json = entityAs[Json]
      root.paths.`/progress`.get.parameters.json.getOption(json) shouldBe parse("""[{
                                                                                  |"name": "id",
                                                                                  |"in": "path",
                                                                                  |"required": true,
                                                                                  |"type": "string",
                                                                                  |"format": "UUID",
                                                                                  |"description" : "The id of the request"
                                                                                  |}]""".stripMargin).toOption
      root.paths.`/progress`.get.responses.json.getOption(json).get.asObject.get.keys.toSet shouldBe Set("200", "404")
      root.paths.`/progress`.get.responses.`200`.schema.`$ref`.string.getOption(json) shouldBe Some("""#/definitions/ResponseIngest""")
    }
  }

  it("documents the post endpoint for progress") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      val json = entityAs[Json]
      root.paths.`/progress`.post.parameters.json.getOption(json) shouldBe parse("""[{
                                                                                  |"name": "progressCreateRequest",
                                                                                  |"in": "body",
                                                                                  |"required": true,
                                                                                  |"description" : "The request to initialise",
                                                                                  |"schema": {
                                                                                  | "$ref": "#/definitions/RequestIngest"
                                                                                  |}
                                                                                  |}]""".stripMargin).toOption
      root.paths.`/progress`.post.responses.json.getOption(json).get.asObject.get.keys.toSet shouldBe Set("201", "400")
      root.paths.`/progress`.post.responses.`201`.schema.`$ref`.string.getOption(json) shouldBe Some("""#/definitions/ResponseIngest""")
      root.paths.`/progress`.post.responses.`201`.headers.Location.json.getOption(json) shouldBe parse("""{
          |"type": "string",
          |"format":"url",
          |"description": "The URL of the created ingest request"
          |}""".stripMargin).toOption
    }
  }

  it("shows the model definition for RequestIngest") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      contentType shouldBe ContentTypes.`application/json`
      val json = entityAs[Json]
      root.definitions.RequestIngest.`type`.string.getOption(json) shouldBe Some("object")
      root.definitions.RequestIngest.properties.json.getOption(json) shouldBe parse(
        """
          |{
          |        "sourceLocation" : {
          |          "$ref" : "#/definitions/Location"
          |        },
          |        "callback" : {
          |          "$ref" : "#/definitions/Callback"
          |        },
          |        "ingestType" : {
          |          "$ref" : "#/definitions/IngestType"
          |        },
          |        "space" : {
          |          "$ref" : "#/definitions/Space"
          |        },
          |        "type" : {
          |          "type" : "string",
          |          "enum" : [
          |             "Ingest"
          |           ]
          |        }
          |      }
        """.stripMargin).toOption
      root.definitions.RequestIngest.required.arr.getOption(json).get.map(_.asString.get) should contain theSameElementsAs List("sourceLocation", "ingestType", "space")

    }
  }

  it("shows the model definition for ResponseIngest") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      contentType shouldBe ContentTypes.`application/json`
      val json = entityAs[Json]
      root.definitions.ResponseIngest.`type`.string.getOption(json) shouldBe Some("object")

      root.definitions.ResponseIngest.required.arr.getOption(json).get.map(_.asString.get) should contain theSameElementsAs List("@context", "id", "space", "sourceLocation", "ingestType", "status", "createdDate", "lastModifiedDate", "events")
      root.definitions.ResponseIngest.properties.`type`.json.getOption(json) shouldBe parse(
        """
          |{
          |"type" : "string",
          |"enum" : [
          |   "Ingest"
          | ]
          |}
        """.stripMargin).toOption

    }
  }

}
