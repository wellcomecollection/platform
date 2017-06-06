package uk.ac.wellcome.platform.idminter

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils._

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with SNSLocal
    with MysqlLocal
    with SQSLocal {
  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
     // "aws.dynamo.identifiers.tableName" -> "identifiers"
    ) ++ snsLocalEndpointFlags ++ sqsLocalFlags ++ mySqlLocalEndpointFlags
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
