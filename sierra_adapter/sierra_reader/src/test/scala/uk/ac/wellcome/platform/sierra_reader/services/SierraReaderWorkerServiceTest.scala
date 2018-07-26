package uk.ac.wellcome.platform.sierra_reader.services

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import uk.ac.wellcome.test.utils.ExtendedPatience
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_reader.modules.WindowManager
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import org.scalatest.compatible.Assertion
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}
import uk.ac.wellcome.platform.sierra_reader.models.{ReaderConfig, SierraConfig, SierraResourceTypes}

import scala.concurrent.ExecutionContext.Implicits.global

class SierraReaderWorkerServiceTest
    extends FunSpec
    with S3
    with SQS
    with Akka
    with Eventually
    with Matchers
    with ExtendedPatience
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
      withMetricsSender(actorSystem) { metricsSender =>
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
              queue,
              metricsSender) { sqsStream =>
              val worker = new SierraReaderWorkerService(
                system = actorSystem,
                sqsStream = sqsStream,
                s3client = s3Client,
                s3Config = S3Config(bucket.name),
                windowManager = new WindowManager(
                  s3Client,
                  S3Config(bucket.name),
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

  // This test isn't actually testing the correct behaviour (see issue 2422:
  // https://github.com/wellcometrust/platform/issues/2422); it's due to be
  // replaced when we fix this behaviour.
  ignore("resumes a window if it finds an in-progress set of records") {
    withSierraReaderWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields",
      resourceType = SierraResourceTypes.items
    ) { fixtures =>
      val prefix = "records_items/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z"

      // First we pre-populate S3 with files as if they'd come from a prior run of the reader.
      s3Client.putObject(fixtures.bucket.name, s"$prefix/0000.json", "[]")
      s3Client.putObject(
        fixtures.bucket.name,
        s"$prefix/0001.json",
        """
          |[
          |  {
          |    "id": "i1794165",
          |    "modifiedDate": "2013-12-10T17:16:35Z",
          |    "data": "{}",
          |    "bibIds": [],
          |  }
          |]
        """.stripMargin
      )

      // Then we trigger the reader, and we expect it to fill in the rest.
      val body =
        """
          |{
          | "start": "2013-12-10T17:16:35Z",
          | "end": "2013-12-13T21:34:35Z"
          |}
        """.stripMargin

      sendNotificationToSQS(queue = fixtures.queue, body = body)

      val pageNames = List("0000.json", "0001.json", "0002.json", "0003.json")
        .map { label =>
          s"$prefix/$label"
        } ++ List(
        "windows_items_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

      eventually {
        // There are 157 item records in the Sierra wiremock so we expect 4 files
        listKeysInBucket(bucket = fixtures.bucket) shouldBe pageNames

        // These two files were pre-populated -- we check the reader hasn't overwritten these
        getItemRecordsFromS3(fixtures.bucket, pageNames(0)) should have size 0
        getItemRecordsFromS3(fixtures.bucket, pageNames(1)) should have size 1

        // We check the reader has filled these in correctly
        getItemRecordsFromS3(fixtures.bucket, pageNames(2)) should have size 50
        getItemRecordsFromS3(fixtures.bucket, pageNames(3)) should have size 7
      }
    }
  }

  private def getBibRecordsFromS3(bucket: Bucket, key: String): List[SierraBibRecord] =
    getObjectFromS3[List[SierraBibRecord]](bucket, key)

  private def getItemRecordsFromS3(bucket: Bucket, key: String): List[SierraItemRecord] =
    getObjectFromS3[List[SierraItemRecord]](bucket, key)

  it(
    "returns a GracefulFailureException if it receives a message that doesn't contain start or end values") {
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
          ex shouldBe a[GracefulFailureException]
      }

    }

  }

  it(
    "does not return a GracefulFailureException if it cannot reach the Sierra API") {
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
            ex shouldNot be(a[GracefulFailureException])
        }
    }
  }

}
