package uk.ac.wellcome.platform.calm_adapter

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.StartupLogbackOverride

class StartupTest extends FeatureTest with StartupLogbackOverride  {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.calmData.streams.appName" -> "calm",
      "aws.dynamo.calmData.streams.arn" -> "calm",
      "aws.dynamo.calmData.tableName" -> "CalmData"
    )
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
