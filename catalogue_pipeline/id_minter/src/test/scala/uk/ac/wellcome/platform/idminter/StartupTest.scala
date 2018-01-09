package uk.ac.wellcome.platform.idminter

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.platform.idminter.utils.{
  IdentifiersTableInfo,
  MysqlLocal
}
import uk.ac.wellcome.test.utils._

class StartupTest
    extends FeatureTest
    with StartupLogbackOverride
    with SNSLocal
    with MysqlLocal
    with IdentifiersTableInfo
    with AmazonCloudWatchFlag
    with SQSLocal {
  val server = new EmbeddedHttpServer(
    new Server(),
    flags = snsLocalFlags ++
      sqsLocalFlags ++
      identifiersMySqlLocalFlags ++
      cloudWatchLocalEndpointFlag
  )

  test("server starts up correctly") {
    server.assertHealthy()
  }
}
