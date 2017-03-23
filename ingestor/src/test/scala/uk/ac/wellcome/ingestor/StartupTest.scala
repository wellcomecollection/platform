package uk.ac.wellcome.platform.ingestor

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.WordSpecFeatureTest

class StartupTest extends WordSpecFeatureTest {

  val server = new EmbeddedHttpServer(stage = Stage.PRODUCTION,
                                      twitterServer = new Server)

  "server" in {
    server.assertHealthy()
  }
}
