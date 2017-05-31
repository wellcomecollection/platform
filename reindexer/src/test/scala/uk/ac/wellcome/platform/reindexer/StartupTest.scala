package uk.ac.wellcome.platform.reindexer

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.StartupLogbackOverride

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      // because StartupTests use real clients that connect to our AWS instances,
      // we cannot give them real values to prevent them to pollute our real AWS instances
      // TODO use real values once we switch to use enpoinds from flags in real client modules
      "aws.dynamo.reindexTracker.tableName" -> "not-real-reindexer-table-name",
      "aws.dynamo.miroData.tableName" -> "not-real-table-name",
      "reindex.target.tableName" -> "MiroData",
      "aws.metrics.namespace" -> "reindexer-startup-tests"
    )
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
