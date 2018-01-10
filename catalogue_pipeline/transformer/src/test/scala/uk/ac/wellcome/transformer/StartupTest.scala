package uk.ac.wellcome.platform.transformer

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils._
import uk.ac.wellcome.transformer.Server

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with AmazonCloudWatchFlag
    with SQSLocal
    with SNSLocal {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "transformer.source" -> "SierraData"
    ) ++ cloudWatchLocalEndpointFlag ++ sqsLocalFlags ++ snsLocalEndpointFlags
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
