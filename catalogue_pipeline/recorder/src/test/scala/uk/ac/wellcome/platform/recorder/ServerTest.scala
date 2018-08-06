package uk.ac.wellcome.platform.recorder

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with fixtures.Server
    with LocalVersionedHybridStore
    with Messaging {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = messageReaderLocalFlags(bucket, queue) ++ vhsLocalFlags(
            bucket,
            table)
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
