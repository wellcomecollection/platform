package uk.ac.wellcome.platform.sierra_items_to_dynamo

import java.time.Instant

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, ExtendedPatience}
import uk.ac.wellcome.dynamo._
import io.circe.generic.extras.semiauto._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemRecord,
  SierraRecord
}
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, SQS}

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
    withLocalDynamoDbTable { tableName =>
      withLocalSqsQueue { queueUrl =>
        val flags = sqsLocalFlags(queueUrl) ++ dynamoDbLocalEndpointFlags(
          tableName)

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
          sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

          eventually {
            Scanamo.scan[SierraItemRecord](dynamoDbClient)(tableName) should have size 1

            val scanamoResult =
              Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)(
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
