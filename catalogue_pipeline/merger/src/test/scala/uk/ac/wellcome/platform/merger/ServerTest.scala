package uk.ac.wellcome.platform.merger

import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.storage.fixtures.S3

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with SNS
    with S3
    with Messaging {

  it("shows the healthcheck message") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalDynamoDbTable { table =>
            withLocalSqsQueue { queue =>
              withServer(queue, topic, storageBucket, messagesBucket, table) {
                server =>
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
