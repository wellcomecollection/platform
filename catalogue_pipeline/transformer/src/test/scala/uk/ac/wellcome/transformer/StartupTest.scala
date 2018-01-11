package uk.ac.wellcome.transformer

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils._

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
      "transformer.source" -> "MiroData"
    ) ++ cloudWatchLocalEndpointFlag ++ sqsLocalFlags ++ snsLocalFlags
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
