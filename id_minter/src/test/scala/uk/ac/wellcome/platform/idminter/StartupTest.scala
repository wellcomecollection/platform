package uk.ac.wellcome.platform.idminter

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{
  DynamoDBLocal,
  SNSLocal,
  StartupLogbackOverride
}

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with SNSLocal
    with DynamoDBLocal {
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
