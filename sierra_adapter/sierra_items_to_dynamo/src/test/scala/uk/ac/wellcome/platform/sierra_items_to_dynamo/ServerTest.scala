package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned

class ServerTest
    extends FunSpec
    with LocalDynamoDbVersioned
    with SQS
    with fixtures.Server {

  it("shows the healthcheck message") {
    withLocalDynamoDbTable { table =>
      withLocalSqsQueue { queue =>
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
