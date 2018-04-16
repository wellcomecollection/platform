package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import io.swagger.util.Json
import io.swagger.models.{Info, Scheme, Swagger}
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.models.ApiVersions

object ApiV1Swagger extends Swagger
object ApiV2Swagger extends Swagger

@Singleton
class SwaggerController @Inject()(
  @Flag("api.scheme") apiScheme: String,
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.host") apiHost: String
) extends Controller {

  prefix(apiPrefix) {
    setupSwaggerEndpoint(ApiVersions.v1, ApiV1Swagger)
    setupSwaggerEndpoint(ApiVersions.v2, ApiV2Swagger)
  }

  private def setupSwaggerEndpoint(version: ApiVersions.Value, swagger: Swagger): Unit = {
    get(s"/$version/swagger.json") { request: Request =>
      response.ok.json(Json.mapper.writeValueAsString(buildSwagger(swagger, apiScheme, apiHost, apiPrefix, version)))
    }
  }

  private def buildSwagger(swagger: Swagger, apiScheme: String, apiHost: String, apiPrefix: String, apiVersion:ApiVersions.Value): Swagger = {
    val scheme = apiScheme match {
      case "https" => Scheme.HTTPS
      case _ => Scheme.HTTP
    }

    swagger.info(
      new Info()
        .description("Search our collections")
        .version(apiVersion.toString)
        .title("Catalogue"))
    swagger.scheme(scheme)
    swagger.host(apiHost)
    swagger.basePath(s"$apiPrefix/$apiVersion")
    swagger.produces("application/json")
    swagger.produces("application/ld+json")
  }
}