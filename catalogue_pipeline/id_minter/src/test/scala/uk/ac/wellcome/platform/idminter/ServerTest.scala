package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.storage.fixtures.S3

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
          withIdentifiersDatabase { identifiersTableConfig =>
            val flags = messagingLocalFlags(bucket, topic, queue) ++
              identifiersLocalDbFlags(identifiersTableConfig)

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
