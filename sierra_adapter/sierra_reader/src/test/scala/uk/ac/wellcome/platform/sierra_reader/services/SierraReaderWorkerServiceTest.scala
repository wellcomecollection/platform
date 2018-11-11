package uk.ac.wellcome.platform.sierra_reader.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import org.scalatest.compatible.Assertion
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.platform.sierra_reader.exceptions.SierraReaderException
import uk.ac.wellcome.platform.sierra_reader.models.{
  ReaderConfig,
  SierraConfig,
  SierraResourceTypes
}

import scala.concurrent.ExecutionContext.Implicits.global

class SierraReaderWorkerServiceTest
    extends FunSpec
    with S3
    with SQS
    with Akka
    with Eventually
    with Matchers
    with IntegrationPatience
    with MetricsSenderFixture
    with ScalaFutures {

  case class FixtureParams(worker: SierraReaderWorkerService,
                           queue: Queue,
                           bucket: Bucket)

  def withSierraReaderWorkerService(
    fields: String,
    apiUrl: String = "http://localhost:8080",
    batchSize: Int = 50,
    resourceType: SierraResourceTypes.Value = SierraResourceTypes.bibs
  )(testWith: TestWith[FixtureParams, Assertion]) = {
    withActorSystem { actorSystem =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          val sierraConfig = SierraConfig(
            resourceType = resourceType,
            apiUrl = apiUrl,
            oauthKey = "key",
            oauthSec = "secret",
            fields = fields
          )

          withSQSStream[NotificationMessage, Assertion](
            actorSystem,
            queue) { sqsStream =>
            val worker = new SierraReaderWorkerService(
              actorSystem = actorSystem,
              sqsStream = sqsStream,
              s3client = s3Client,
              s3Config = createS3ConfigWith(bucket),
              windowManager = new WindowManager(
                s3Client,
                createS3ConfigWith(bucket),
                sierraConfig = sierraConfig
              ),
              readerConfig = ReaderConfig(batchSize = batchSize),
              sierraConfig = sierraConfig
            )

            testWith(FixtureParams(worker, queue, bucket))
          }
        }
      }
    }
  }

  it(
    "reads a window message from SQS, retrieves the bibs from Sierra and writes them to S3") {
    val body =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    withSierraReaderWorkerService(
      fields = "updatedDate,deletedDate,deleted,suppressed,author,title",
      batchSize = 10,
      resourceType = SierraResourceTypes.bibs
    ) { fixtures =>
      sendNotificationToSQS(queue = fixtures.queue, body = body)

      val pageNames = List("0000.json", "0001.json", "0002.json").map { label =>
        s"records_bibs/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z/$label"
      } ++ List(
        "windows_bibs_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

      eventually {
        // there are 29 bib updates in the sierra wiremock so we expect 3 files
        listKeysInBucket(bucket = fixtures.bucket) shouldBe pageNames

        getBibRecordsFromS3(fixtures.bucket, pageNames(0)) should have size 10
        getBibRecordsFromS3(fixtures.bucket, pageNames(1)) should have size 10
        getBibRecordsFromS3(fixtures.bucket, pageNames(2)) should have size 9
      }
    }
  }

  it(
    "reads a window message from SQS, retrieves the items from Sierra and writes them to S3") {
    val body =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    withSierraReaderWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields",
      resourceType = SierraResourceTypes.items
    ) { fixtures =>
      sendNotificationToSQS(queue = fixtures.queue, body = body)

      val pageNames = List("0000.json", "0001.json", "0002.json", "0003.json")
        .map { label =>
          s"records_items/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z/$label"
        } ++ List(
        "windows_items_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

      eventually {
        // There are 157 item records in the Sierra wiremock so we expect 4 files
        listKeysInBucket(bucket = fixtures.bucket) shouldBe pageNames

        getItemRecordsFromS3(fixtures.bucket, pageNames(0)) should have size 50
        getItemRecordsFromS3(fixtures.bucket, pageNames(1)) should have size 50
        getItemRecordsFromS3(fixtures.bucket, pageNames(2)) should have size 50
        getItemRecordsFromS3(fixtures.bucket, pageNames(3)) should have size 7
      }
    }
  }

  it("resumes a window if it finds an in-progress set of records") {
    withSierraReaderWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields",
      resourceType = SierraResourceTypes.items
    ) { fixtures =>
      val body =
        """
          |{
          | "start": "2013-12-10T17:16:35Z",
          | "end": "2013-12-13T21:34:35Z"
          |}
        """.stripMargin

      // Do a complete run of the reader -- this gives us a set of JSON files
      // to compare to.
      sendNotificationToSQS(queue = fixtures.queue, body = body)

      eventually {
        assertQueueEmpty(queue = fixtures.queue)

        // There are 157 item records in the Sierra wiremock, so we expect
        // 5 files -- the four JSON files, and a window marker.
        listKeysInBucket(bucket = fixtures.bucket) should have size 5
      }

      val expectedContents = getAllObjectContents(bucket = fixtures.bucket)

      // Now, let's delete every key in the bucket _except_ the first --
      // which we'll use to restart the window.
      listKeysInBucket(bucket = fixtures.bucket)
        .filterNot { _.endsWith("0000.json") }
        .foreach { key =>
          s3Client.deleteObject(fixtures.bucket.name, key)
        }

      eventually {
        listKeysInBucket(bucket = fixtures.bucket) should have size 1
      }

      // Now, send a second message to the reader, and we'll see if it completes
      // the window successfully.
      sendNotificationToSQS(queue = fixtures.queue, body = body)

      eventually {
        getAllObjectContents(bucket = fixtures.bucket) shouldBe expectedContents
      }
    }
  }

  private def getBibRecordsFromS3(bucket: Bucket,
                                  key: String): List[SierraBibRecord] =
    getObjectFromS3[List[SierraBibRecord]](bucket, key)

  private def getItemRecordsFromS3(bucket: Bucket,
                                   key: String): List[SierraItemRecord] =
    getObjectFromS3[List[SierraItemRecord]](bucket, key)

  it(
    "returns a SierraReaderException if it receives a message that doesn't contain start or end values") {
    withSierraReaderWorkerService(fields = "") { fixtures =>
      val body =
        """
          |{
          | "start": "2013-12-10T17:16:35Z"
          |}
        """.stripMargin

      val notificationMessage = createNotificationMessageWith(body = body)
      whenReady(fixtures.worker.processMessage(notificationMessage).failed) {
        ex =>
          ex shouldBe a[SierraReaderException]
      }

    }

  }

  it("doesn't return a SierraReaderException if it cannot reach the Sierra API") {
    withSierraReaderWorkerService(fields = "", apiUrl = "http://localhost:5050") {
      fixtures =>
        val body =
          """
          |{
          | "start": "2013-12-10T17:16:35Z",
          | "end": "2013-12-13T21:34:35Z"
          |}
        """.stripMargin

        val notificationMessage = createNotificationMessageWith(body = body)

        whenReady(fixtures.worker.processMessage(notificationMessage).failed) {
          ex =>
            ex shouldNot be(a[SierraReaderException])
        }
    }
  }

}
