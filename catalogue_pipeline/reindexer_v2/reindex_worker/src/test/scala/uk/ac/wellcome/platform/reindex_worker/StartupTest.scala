package uk.ac.wellcome.platform.reindex_worker

import com.google.inject.Stage
import com.gu.scanamo.DynamoFormat
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, StartupLogbackOverride}

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with DynamoDBLocal[HybridRecord]
    with AmazonCloudWatchFlag {

  implicit val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  override lazy val tableName = "reindexer-startup-test-table"

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.tableName" -> tableName,
      "aws.region" -> "eu-west-1"
    ) ++ dynamoDbLocalEndpointFlags ++ cloudWatchLocalEndpointFlag
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
