package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with LocalVersionedHybridStore
    with fixtures.Server
    with SQS {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { itemsBucket =>
        withLocalS3Bucket { vhsBucket =>
          withLocalDynamoDbTable { table =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(vhsBucket, table) ++ s3LocalFlags(itemsBucket)
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
