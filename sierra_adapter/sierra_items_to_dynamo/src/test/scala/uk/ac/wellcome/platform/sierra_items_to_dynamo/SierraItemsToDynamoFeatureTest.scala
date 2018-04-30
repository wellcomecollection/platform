package uk.ac.wellcome.platform.sierra_items_to_dynamo

import java.time.Instant

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecord}
import uk.ac.wellcome.test.fixtures.LocalDynamoDb

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with LocalDynamoDb[SierraItemRecord]
    with SQS
    with fixtures.Server
    with Matchers
    with Eventually
    with ExtendedPatience {

  override lazy val evidence: DynamoFormat[SierraItemRecord] =
    DynamoFormat[SierraItemRecord]

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalSqsQueue { queue =>
        val flags = sqsLocalFlags(queue) ++ dynamoDbLocalEndpointFlags(table)

        withServer(flags) { server =>
          val id = "i12345"
          val bibId = "b54321"
          val data = s"""{"id": "$id", "bibIds": ["$bibId"]}"""
          val modifiedDate = Instant.ofEpochSecond(Instant.now.getEpochSecond)
          val message = SierraRecord(id, data, modifiedDate)

          val sqsMessage =
            SQSMessage(
              Some("subject"),
              toJson(message).get,
              "topic",
              "messageType",
              "timestamp")
          sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

          eventually {
            Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

            val scanamoResult =
              Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
                'id -> id)

            scanamoResult shouldBe defined
            scanamoResult.get shouldBe Right(
              SierraItemRecord(
                id,
                data,
                modifiedDate,
                List(bibId),
                version = 1))
          }
        }
      }
    }
  }
}
