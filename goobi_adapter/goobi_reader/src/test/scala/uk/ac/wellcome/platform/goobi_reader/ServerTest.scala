package uk.ac.wellcome.platform.goobi_reader

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with LocalVersionedHybridStore {

  it("it shows the healthcheck message") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withLocalSqsQueue { queue =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table, "goobi")

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
