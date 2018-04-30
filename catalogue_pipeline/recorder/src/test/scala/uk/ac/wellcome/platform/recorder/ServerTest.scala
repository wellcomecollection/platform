package uk.ac.wellcome.platform.recorder

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec

class ServerTest extends FunSpec with fixtures.Server {

  it("shows the healthcheck message") {
    withServer(flags = Map()) { server =>
      server.httpGet(path = "/management/healthcheck",
        andExpect = Ok,
        withJsonBody = """{"message": "ok"}""")
    }
  }
}
