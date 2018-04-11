package uk.ac.wellcome.platform.sierra_bib_merger

import com.gu.scanamo.DynamoFormat
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, S3, SQS}

class ServerTest
    extends FunSpec
    with LocalDynamoDb[HybridRecord]
    with fixtures.Server
    with S3
    with SQS {

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { tableName =>
          val flags = sqsLocalFlags(queue) ++ s3LocalFlags(bucket) ++ dynamoDbLocalEndpointFlags(
            tableName)
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
