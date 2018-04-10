package uk.ac.wellcome.platform.snapshot_convertor

import com.twitter.finagle.http.Status.Ok
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{S3, SNS, SQS}

class ServerTest
    extends FunSpec
    with S3
    with SNS
    with SQS
    with ScalaFutures
    with fixtures.Server {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queueUrl) ++ s3LocalFlags(
            bucket)
          withServer(flags) { server =>
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
