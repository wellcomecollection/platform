package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, S3Local, SQSLocal}

class ServerTest extends FeatureTest
  with AmazonCloudWatchFlag
  with S3Local
  with SQSLocal {

  override lazy val bucketName = "sierra-reader-servertest-bucket"

  val server = new EmbeddedHttpServer(
    new Server(),
    Map(
      "reader.resourceType" -> "bibs"
    ) ++ s3LocalFlags ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag
  )

  test("it shows the healthcheck message") {
    server.httpGet(
      path = "/management/healthcheck",
      andExpect = Ok,
      withJsonBody = """{"message": "ok"}""")
  }
}
