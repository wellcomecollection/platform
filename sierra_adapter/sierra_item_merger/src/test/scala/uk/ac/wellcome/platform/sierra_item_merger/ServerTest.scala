package uk.ac.wellcome.platform.sierra_item_merger

import com.gu.scanamo.DynamoFormat
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, S3, SqsFixtures}

class ServerTest
  extends FunSpec
    with LocalDynamoDb[HybridRecord]
    with fixtures.Server
    with S3
    with SqsFixtures {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  it("shows the healthcheck message") {
    withLocalSqsQueue { queueUrl =>
      withLocalS3Bucket { bucketName =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ dynamoDbLocalEndpointFlags(tableName)
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
