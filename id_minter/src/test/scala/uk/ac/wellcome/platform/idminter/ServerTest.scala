package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.platform.idminter.utils.{IdentifiersTableInfo, MysqlLocal}
import uk.ac.wellcome.test.utils.{SNSLocal, SQSLocal}

class ServerTest
    extends FeatureTest
    with SNSLocal
    with MysqlLocal
    with IdentifiersTableInfo
    with SQSLocal {

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = snsLocalEndpointFlags ++ identifiersMySqlLocalFlags ++ sqsLocalFlags
  )

  test("it should show the healthcheck message") {
    server.httpGet(path = "/management/healthcheck",
                   andExpect = Ok,
                   withJsonBody = """{"message": "ok"}""")
  }
}
