package uk.ac.wellcome.platform.ingestor

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{SQSLocal, StartupLogbackOverride}

class StartupTest extends FeatureTest with StartupLogbackOverride with SQSLocal {

  val server = new EmbeddedHttpServer(stage = Stage.PRODUCTION,
                                      twitterServer = new Server, flags = sqsLocalFlags)

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
