package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.gu.scanamo.DynamoFormat
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, SqsFixtures}

import uk.ac.wellcome.dynamo._


class ServerTest
    extends FunSpec
      with LocalDynamoDb[SierraItemRecord]
      with SqsFixtures
      with fixtures.Server {

  override lazy val evidence = DynamoFormat[SierraItemRecord]

  it("shows the healthcheck message") {
    withLocalDynamoDbTable { tableName =>
      withLocalSqsQueue { queueUrl =>

        val flags = sqsLocalFlags(queueUrl) ++ dynamoDbLocalEndpointFlags(tableName) ++ Map(
          "aws.dynamo.tableName" -> tableName
        )

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
