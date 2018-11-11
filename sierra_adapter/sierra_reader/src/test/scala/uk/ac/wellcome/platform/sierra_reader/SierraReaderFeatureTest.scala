package uk.ac.wellcome.platform.sierra_reader

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.platform.sierra_reader.config.models.{ReaderConfig, SierraAPIConfig}
import uk.ac.wellcome.platform.sierra_reader.models.SierraResourceTypes
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

class SierraReaderFeatureTest
    extends FunSpec
    with Akka
    with S3
    with SQS
    with Eventually
    with Matchers
    with IntegrationPatience {

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

  private def withWorkerService[R](bucket: Bucket, queue: Queue)(testWith: TestWith[SierraReaderWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withSQSStream[NotificationMessage, R](actorSystem, queue) { sqsStream =>
        val workerService = new SierraReaderWorkerService(
          actorSystem = actorSystem,
          sqsStream = sqsStream,
          s3client = s3Client,
          s3Config = createS3ConfigWith(bucket),
          readerConfig = ReaderConfig(
            resourceType = SierraResourceTypes.bibs,
            fields = "updatedDate,deletedDate,deleted,suppressed,author,title",
            batchSize = 50
          ),
          sierraAPIConfig = SierraAPIConfig(
            apiURL = "http://localhost:8080",
            oauthKey = "key",
            oauthSec = "secret"
          )
        )

        testWith(workerService)
      }
    }
}
