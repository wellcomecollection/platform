package uk.ac.wellcome.platform.sierra_reader

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.fixtures.{S3, SqsFixtures}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class SierraReaderFeatureTest
    extends FunSpec
    with fixtures.Server
    with S3
    with SqsFixtures
    with Eventually
    with Matchers
    with ExtendedPatience {

  it("reads bibs from Sierra and writes files to S3") {
    withLocalS3Bucket { bucketName =>
      withLocalSqsQueue { queueUrl =>
        val flags = s3LocalFlags(bucketName) ++ sqsLocalFlags(queueUrl)

        withServer(flags) { _ =>
          val message =
            """
            |{
            | "start": "2013-12-10T17:16:35Z",
            | "end": "2013-12-13T21:34:35Z"
            |}
            """.stripMargin

          val sqsMessage = SQSMessage(
            Some("subject"),
            message,
            "topic",
            "messageType",
            "timestamp")
          sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

          eventually {
            // This comes from the wiremock recordings for Sierra API response
            s3Client
              .listObjects(bucketName)
              .getObjectSummaries should have size 2
          }
        }
      }
    }
  }
}
