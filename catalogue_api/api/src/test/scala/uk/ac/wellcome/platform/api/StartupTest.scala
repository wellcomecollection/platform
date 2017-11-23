package uk.ac.wellcome.platform.api

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class StartupTest extends FeatureTest {

  val server = new EmbeddedHttpServer(stage = Stage.PRODUCTION,
                                      twitterServer = new Server)

  test("server should start up correctly") {
    server.assertHealthy()
  }
}
