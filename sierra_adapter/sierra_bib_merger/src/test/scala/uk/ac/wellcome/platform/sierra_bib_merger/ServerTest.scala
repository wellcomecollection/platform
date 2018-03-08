package uk.ac.wellcome.platform.sierra_bib_merger

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}

class ServerTest extends FeatureTest with AmazonCloudWatchFlag with SQSLocal {

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.dynamo.tableName" -> "serverTestTable"
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag
  )

  test("it shows the healthcheck message") {
    server.httpGet(
      path = "/management/healthcheck",
      andExpect = Ok,
      withJsonBody = """{"message": "ok"}""")
  }
}
