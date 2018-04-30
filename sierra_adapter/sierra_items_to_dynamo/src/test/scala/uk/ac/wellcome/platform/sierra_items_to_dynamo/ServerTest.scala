package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.gu.scanamo.DynamoFormat
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.text.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.test.fixtures.LocalDynamoDb

import uk.ac.wellcome.dynamo._

class ServerTest
    extends FunSpec
    with LocalDynamoDb[SierraItemRecord]
    with SQS
    with fixtures.Server {

  override lazy val evidence = DynamoFormat[SierraItemRecord]

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
