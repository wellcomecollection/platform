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
    with SNSLocal
    with S3Local {
  override lazy val bucketName: String = "startup-bucket"
  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = cloudWatchLocalEndpointFlag ++ sqsLocalFlags ++ snsLocalFlags ++ s3LocalFlags
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
