package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag

@Singleton
class ManagementController @Inject()(
                                    @Flag("api.name") apiName: String
) extends Controller {

  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("name" -> apiName))
  }
}
