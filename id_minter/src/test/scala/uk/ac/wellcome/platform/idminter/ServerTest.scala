package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{DynamoDBLocal, SNSLocal, SQSLocal}

class ServerTest
    extends FeatureTest
    with SNSLocal
    with DynamoDBLocal
    with SQSLocal {

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.dynamo.identifiers.tableName" -> "identifiers"
    ) ++ snsLocalEndpointFlags ++ dynamoDbTestEndpointFlags ++ sqsLocalFlags
  )

  test("it should show the healthcheck message") {
    server.httpGet(path = "/management/healthcheck",
                   andExpect = Ok,
                   withJsonBody = """{"message": "ok"}""")
  }
}
