package uk.ac.wellcome.platform.ingestor

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  SQSLocal,
  StartupLogbackOverride
}

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with SQSLocal
    with AmazonCloudWatchFlag {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = sqsLocalFlags ++ cloudWatchLocalEndpointFlag)

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
