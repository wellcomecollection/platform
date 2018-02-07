package uk.ac.wellcome.platform.sierra_bib_merger

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ServerTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.dynamo.tableName" -> "serverTestTable"
    )
  )

  test("it shows the healthcheck message") {
    server.httpGet(path = "/management/healthcheck",
                   andExpect = Ok,
                   withJsonBody = """{"message": "ok"}""")
  }
}
