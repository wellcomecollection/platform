package uk.ac.wellcome.platform.reindex.processor

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned

class ServerTest extends FunSpec with fixtures.Server with SQS with LocalDynamoDbVersioned {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        val flags = sqsLocalFlags(queue) ++ dynamoDbLocalEndpointFlags(table)
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
