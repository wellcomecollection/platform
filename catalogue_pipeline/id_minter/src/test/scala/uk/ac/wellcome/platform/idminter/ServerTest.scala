package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.storage.test.fixtures.S3

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.IdentifiersDatabase {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { dbConfig =>
            val flags = sqsLocalFlags(queue) ++ messagingLocalFlags(
              bucket,
              topic) ++
              dbConfig.flags

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
