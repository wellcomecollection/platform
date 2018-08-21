package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with LocalVersionedHybridStore
    with SNS
    with SQS
    with fixtures.Server {

  it("shows the healthcheck message") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueue { queue =>
          withLocalSnsTopic { topic =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table) ++ snsLocalFlags(topic)

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
}
