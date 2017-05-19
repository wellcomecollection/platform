package uk.ac.wellcome.platform.idminter

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.finagle.http.Status._
import uk.ac.wellcome.test.utils.StartupLogbackOverride

class StartupTest extends FeatureTest with StartupLogbackOverride {
  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.dynamo.identifiers.tableName" -> "identifiers"
    )
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
