package uk.ac.wellcome.platform.reindexer

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.test.utils.{DynamoDBLocal, MetricsSenderLocal, StartupLogbackOverride}

class StartupTest
    extends FeatureTest
    with DynamoDBLocal
    with StartupLogbackOverride
    with MetricsSenderLocal {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
      "aws.dynamo.miroData.tableName" -> "MiroData",
      "reindex.target.tableName" -> "MiroData"
    )
  ).bind[AmazonDynamoDB](dynamoDbClient)
    .bind[MetricsSender](metricsSender)

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
