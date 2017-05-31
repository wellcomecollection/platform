package uk.ac.wellcome.platform.transformer

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  DynamoDBLocal,
  StartupLogbackOverride
}

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with AmazonCloudWatchFlag
    with DynamoDBLocal {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.miroData.streams.appName" -> "miro",
      "aws.dynamo.miroData.streams.arn" -> "miro",
      "aws.dynamo.miroData.tableName" -> "MiroData"
    ) ++ testFlags
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
