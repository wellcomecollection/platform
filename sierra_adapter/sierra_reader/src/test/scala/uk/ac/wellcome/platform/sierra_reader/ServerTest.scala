package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finagle.http.Status._
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.test.fixtures.{S3, SqsFixtures}

class ServerTest
    extends FeatureTest
    with fixtures.Server
    with S3
    with SqsFixtures {

  test("it shows the healthcheck message") {
    withLocalS3Bucket { bucketName =>
      withLocalSqsQueue { queueUrl =>
        val flags = s3LocalFlags(bucketName) ++ sqsLocalFlags(queueUrl)

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
