package uk.ac.wellcome.platform.idminter

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.finagle.http.Status._

class StartupTest extends FeatureTest {

  val server = new EmbeddedHttpServer(stage = Stage.PRODUCTION,
                                      twitterServer = new Server)

  test("server") {
    server.assertHealthy()
  }
}
