package uk.ac.wellcome.platform.idminter

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures._

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with SNS
    with S3
    with fixtures.IdentifiersDatabase {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withIdentifiersDatabase { dbConfig =>
            val flags = sqsLocalFlags(queue) ++ snsLocalFlags(topic) ++ s3LocalFlags(bucket) ++ dbConfig.flags

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
