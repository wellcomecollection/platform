package uk.ac.wellcome.platform.sierra_reader

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience

class SierraReaderFeatureTest
    extends FunSpec
    with fixtures.Server
    with S3
    with SQS
    with Eventually
    with Matchers
    with ExtendedPatience {

  it("reads bibs from Sierra and writes files to S3") {
    withLocalS3Bucket { bucket =>
      withLocalSqsQueue { queue =>
        val flags = s3LocalFlags(bucket) ++ sqsLocalFlags(queue) ++ Map(
          "reader.resourceType" -> "bibs",
          "sierra.apiUrl" -> "http://localhost:8080",
          "sierra.oauthKey" -> "key",
          "sierra.oauthSecret" -> "secret",
          "sierra.fields" -> "updatedDate,deletedDate,deleted,suppressed,author,title"
        )

        val body =
          """
            |{
            | "start": "2013-12-10T17:16:35Z",
            | "end": "2013-12-13T21:34:35Z"
            |}
          """.stripMargin

        withServer(flags) { _ =>
          sendNotificationToSQS(queue = queue, body = body)

          eventually {
            // This comes from the wiremock recordings for Sierra API response
            listKeysInBucket(bucket = bucket) should have size 2
          }
        }
      }
    }
  }
}
