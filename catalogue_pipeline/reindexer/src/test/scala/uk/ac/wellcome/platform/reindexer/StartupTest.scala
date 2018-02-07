package uk.ac.wellcome.platform.reindexer

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, StartupLogbackOverride}

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with DynamoDBLocal
    with AmazonCloudWatchFlag {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.tableName" -> "ReindexTracker",
      "reindex.target.tableName" -> "MiroData",
      "reindex.target.reindexShard" -> "default"
    ) ++ dynamoDbLocalEndpointFlags ++ cloudWatchLocalEndpointFlag
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
