package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.StartupLogbackOverride

class StartupTest extends FeatureTest with StartupLogbackOverride {

  val server = new EmbeddedHttpServer(new Server())

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
