package uk.ac.wellcome.platform.recorder

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore

class ServerTest
  extends FunSpec
    with fixtures.Server
    with LocalVersionedHybridStore
    with SQS {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ s3LocalFlags(bucket) ++ dynamoDbLocalEndpointFlags(table)
          withServer(flags) { server =>
            server.httpGet(path = "/management/healthcheck",
              andExpect = Ok,
              withJsonBody = """{"message": "ok"}""")
          }
        }
      }
    }
  }
}
