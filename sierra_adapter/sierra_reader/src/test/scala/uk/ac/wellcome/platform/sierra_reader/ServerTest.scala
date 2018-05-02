package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finagle.http.Status._
import com.twitter.inject.server.FeatureTest
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.test.fixtures.S3

class ServerTest extends FunSpec with fixtures.Server with S3 with SQS {

  it("it shows the healthcheck message") {
    withLocalS3Bucket { bucket =>
      withLocalSqsQueue { queue =>
        val flags = s3LocalFlags(bucket) ++ sqsLocalFlags(queue) ++ Map(
          "reader.resourceType" -> "bibs",
          "sierra.apiUrl" -> "http://localhost:8080",
          "sierra.oauthKey" -> "key",
          "sierra.oauthSecret" -> "secret",
          "sierra.fields" -> "updatedDate,deletedDate,deleted,suppressed,author,title"
        )

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
