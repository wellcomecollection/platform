package uk.ac.wellcome.platform.snapshot_convertor

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.AmazonCloudWatchFlag

class ServerTest
    extends FunSpec
    with AmazonCloudWatchFlag
    with SQS
    with SNS
    with S3
    with ScalaFutures {

  def withServer[R](queueUrl: String, topicArn: String, bucketName: String)(
    testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = snsLocalFlags(topicArn) ++ cloudWatchLocalEndpointFlag ++ sqsLocalFlags(
          queueUrl) ++ s3LocalFlags(bucketName)
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  it("shows the healthcheck message") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalS3Bucket { bucketName =>
          withServer(queueUrl, topicArn, bucketName) { server =>
            server.httpGet(
              path = "/management/healthcheck",
              andExpect = Ok,
              withJsonBody = """{"message": "ok"}""")

          }
        }
      }
    }
  }
}
