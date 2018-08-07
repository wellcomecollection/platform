package uk.ac.wellcome.platform.reindex.processor

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with LocalDynamoDbVersioned {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withServer(sqsLocalFlags(queue) ++ dynamoClientLocalFlags) { server =>
          server.httpGet(
            path = "/management/healthcheck",
            andExpect = Ok,
            withJsonBody = """{"message": "ok"}""")
        }
      }
    }
  }
}
