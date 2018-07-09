package uk.ac.wellcome.platform.reindex_request_processor

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec

class ServerTest extends FunSpec with fixtures.Server {

  it("shows the healthcheck message") {
    withServer(Map()) { server =>
      server.httpGet(
        path = "/management/healthcheck",
        andExpect = Ok,
        withJsonBody = """{"message": "ok"}""")
    }
  }
}
