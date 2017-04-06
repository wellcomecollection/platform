package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ServerTest extends FeatureTest {

  val server = new EmbeddedHttpServer(new Server)

  test("it should show the healthcheck message") {
    server.httpGet(path = "/management/healthcheck", andExpect = Ok, withJsonBody = """{"message": "ok"}""")
  }
}
