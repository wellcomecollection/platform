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
      println(json)
      root.paths.`/progress`.get.parameters.json.getOption(json) shouldBe parse("""[{
                                                                                  |"name": "id",
                                                                                  |"in": "path",
                                                                                  |"required": true,
                                                                                  |"type": "string",
                                                                                  |"description" : "The id of the request"
                                                                                  |}]""".stripMargin).toOption
      root.paths.`/progress`.get.responses.json.getOption(json).get.asObject.get.keys.toSet shouldBe Set("200", "404")
      root.paths.`/progress`.get.responses.`200`.schema.`$ref`.string.getOption(json) shouldBe Some("""#/definitions/ResponseDisplayIngest""")
    }
  }

  it("documents the post endpoint for progress") {
    Get("/progress/swagger.json") ~> SwaggerDocService.routes ~> check {
      val json = entityAs[Json]
      println(json)
      root.paths.`/progress`.post.parameters.json.getOption(json) shouldBe parse("""[{
                                                                                  |"name": "progressCreateRequest",
                                                                                  |"in": "body",
                                                                                  |"required": true,
                                                                                  |"description" : "The request to initialise",
                                                                                  |"schema": {
                                                                                  | "$ref": "#/definitions/RequestDisplayIngest"
                                                                                  |}
                                                                                  |}]""".stripMargin).toOption
      root.paths.`/progress`.post.responses.json.getOption(json).get.asObject.get.keys.toSet shouldBe Set("201", "400")
      root.paths.`/progress`.post.responses.`201`.schema.`$ref`.string.getOption(json) shouldBe Some("""#/definitions/ResponseDisplayIngest""")
      root.paths.`/progress`.post.responses.`201`.headers.Location.json.getOption(json) shouldBe parse("""{
          |"type": "string",
          |"format":"url",
          |"description": "The URL of the created ingest request"
          |}""".stripMargin).toOption
    }
  }

}
