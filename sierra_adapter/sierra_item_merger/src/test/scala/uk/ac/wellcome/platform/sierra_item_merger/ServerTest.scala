package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with LocalVersionedHybridStore
    with fixtures.Server
    with Messaging {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { itemsToDynamoBucket =>
        withLocalS3Bucket { sierraDataBucket =>
          withLocalDynamoDbTable { table =>
            withLocalSnsTopic { topic =>
              val flags = s3LocalFlags(itemsToDynamoBucket) ++ sqsLocalFlags(queue) ++ vhsLocalFlags(
                sierraDataBucket,
                table) ++ snsLocalFlags(topic)
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
}
