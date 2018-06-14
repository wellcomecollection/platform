package uk.ac.wellcome.platform.merger

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.storage.test.fixtures.S3

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with SNS
    with S3
    with Messaging {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withServer(queue) { server =>
        server.httpGet(
          path = "/management/healthcheck",
          andExpect = Ok,
          withJsonBody = """{"message": "ok"}""")
      }
    }
  }
}
