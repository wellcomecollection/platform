package uk.ac.wellcome.platform.mets_reader

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS

class ServerTest extends FunSpec with fixtures.Server with SQS {

  it("it shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      val flags = sqsLocalFlags(queue)

      withServer(flags) { server =>
        server.httpGet(
          path = "/management/healthcheck",
          andExpect = Ok,
          withJsonBody = """{"message": "ok"}""")
      }
    }
  }
}
