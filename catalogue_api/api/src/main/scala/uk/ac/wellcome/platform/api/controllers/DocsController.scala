package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import io.swagger.util.Json
import io.swagger.models.{Info, Scheme, Swagger}

import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.platform.api.models.ApiConfig

object ApiV1Swagger extends Swagger
object ApiV2Swagger extends Swagger

@Singleton
class DocsController @Inject()(apiConfig: ApiConfig) extends Controller {
  prefix(apiConfig.pathPrefix) {
    setupSwaggerEndpoint(ApiVersions.v1, ApiV1Swagger)
    setupSwaggerEndpoint(ApiVersions.v2, ApiV2Swagger)
  }

  private def setupSwaggerEndpoint(version: ApiVersions.Value,
                                   swagger: Swagger): Unit = {
    get(s"/$version/swagger.json") { request: Request =>
      response.ok.json(
        Json.mapper.writeValueAsString(
          buildSwagger(swagger, apiConfig = apiConfig, apiVersion = version)))
    }
  }

  private def buildSwagger(swagger: Swagger,
                           apiConfig: ApiConfig,
                           apiVersion: ApiVersions.Value): Swagger = {
    val scheme = apiConfig.scheme match {
      case "https" => Scheme.HTTPS
      case _       => Scheme.HTTP
    }

    swagger.info(
      new Info()
        .description("Search our collections")
        .version(apiVersion.toString)
        .title("Catalogue"))
    swagger.scheme(scheme)
    swagger.host(apiConfig.host)
    // Had to remove the basePath because of this "improvement"
    // https://github.com/jakehschwartz/finatra-swagger/pull/27
    // all paths now are including the prefix which means they
    // are relative to the host, not to the basePath
    swagger.produces("application/json")
    swagger.produces("application/ld+json")
  }
}
