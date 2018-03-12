package uk.ac.wellcome.platform.sierra_bib_merger

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  DynamoDBLocalClients,
  S3Local,
  SQSLocal
}

class ServerTest
    extends FeatureTest
    with AmazonCloudWatchFlag
    with DynamoDBLocalClients
    with S3Local
    with SQSLocal {

 override lazy val bucketName = "sierra-bib-merger-servertest-bucket"
 val queueUrl = createQueueAndReturnUrl("sierra-bib-merger-servertest-q")

  val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.dynamo.tableName" -> "serverTestTable",
       "aws.sqs.queue.url" -> queueUrl
    ) ++ s3LocalFlags ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  test("it shows the healthcheck message") {
    server.httpGet(
      path = "/management/healthcheck",
      andExpect = Ok,
      withJsonBody = """{"message": "ok"}""")
  }
}
