package uk.ac.wellcome.platform.sierra_reader

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.platform.sierra_reader.fixtures.WorkerServiceFixture
import uk.ac.wellcome.storage.fixtures.S3

class SierraReaderFeatureTest
    extends FunSpec
    with S3
    with SQS
    with Eventually
    with Matchers
    with IntegrationPatience
    with WorkerServiceFixture {

  it("reads bibs from Sierra and writes files to S3") {
    withLocalS3Bucket { bucket =>
      withLocalSqsQueue { queue =>
        withWorkerService(bucket, queue) { service =>
          service.run()

          val body =
            """
              |{
              | "start": "2013-12-10T17:16:35Z",
              | "end": "2013-12-13T21:34:35Z"
              |}
            """.stripMargin

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
