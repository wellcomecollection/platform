package uk.ac.wellcome.platform.reindexer

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
    with DynamoDBLocal
    with AmazonCloudWatchFlag {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
      "aws.dynamo.miroData.tableName" -> "MiroData",
      "reindex.target.tableName" -> "MiroData",
      "reindex.target.reindexShard" -> "default"
    ) ++ dynamoDbLocalEndpointFlags ++ cloudWatchLocalEndpointFlag
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
