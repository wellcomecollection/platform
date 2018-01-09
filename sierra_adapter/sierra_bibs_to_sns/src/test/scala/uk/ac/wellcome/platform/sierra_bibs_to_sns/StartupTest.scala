package uk.ac.wellcome.platform.sierra_bibs_to_sns

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.StartupLogbackOverride

class StartupTest extends FeatureTest with StartupLogbackOverride {

  val server = new EmbeddedHttpServer(
    twitterServer = new Server,
    flags = Map(),
    stage = Stage.PRODUCTION
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
