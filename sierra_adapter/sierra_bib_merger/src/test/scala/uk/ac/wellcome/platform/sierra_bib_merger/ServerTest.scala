package uk.ac.wellcome.platform.sierra_bib_merger

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

class ServerTest
    extends FunSpec
    with LocalVersionedHybridStore
    with fixtures.Server
    with SQS with SNS {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
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
