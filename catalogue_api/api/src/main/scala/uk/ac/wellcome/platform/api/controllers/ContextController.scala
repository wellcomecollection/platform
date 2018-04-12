package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}

@Singleton
class ContextController @Inject()(
  @Flag("api.prefix") apiPrefix: String
) extends Controller {

  prefix(apiPrefix) {

    get("/v1/context.json") { request: Request =>
      response.ok.file("context.json")
    }

  }
}
