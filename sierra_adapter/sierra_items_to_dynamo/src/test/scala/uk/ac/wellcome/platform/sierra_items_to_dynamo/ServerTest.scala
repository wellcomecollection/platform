package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  DynamoDBLocalClients,
  SQSLocal
}

class ServerTest
    extends FeatureTest
    with AmazonCloudWatchFlag
    with DynamoDBLocalClients
    with SQSLocal {

  val queueUrl = createQueueAndReturnUrl("sierra-item-to-dynamo-servertest-q")

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags("serverTestTable")
  )

  test("it shows the healthcheck message") {
    server.httpGet(
      path = "/management/healthcheck",
      andExpect = Ok,
      withJsonBody = """{"message": "ok"}""")
  }
}
