package uk.ac.wellcome.platform.sierra_reader.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.{ExtendedPatience, S3Local, SQSLocal}

import scala.collection.JavaConversions._
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import uk.ac.wellcome.platform.sierra_reader.flow.SierraResourceTypes
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.platform.sierra_reader.modules.WindowManager

import scala.concurrent.Future
import scala.concurrent.duration._

class SierraReaderWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with S3Local
    with SQSLocal
    with Eventually
    with Matchers
    with ExtendedPatience
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  override lazy val bucketName = "sierra-reader-test-bucket"

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  val mockMetrics = new MetricsSender("namespace", mockCloudWatch)

  var worker: Option[SierraReaderWorkerService] = None
  val actorSystem = ActorSystem()

  override def beforeEach(): Unit = {
    super.beforeEach()
    stopWorker(worker)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  private def createSierraReaderWorkerService(
    fields: String,
    apiUrl: String = "http://localhost:8080",
    batchSize: Int = 50,
    resourceType: SierraResourceTypes.Value = SierraResourceTypes.bibs
  ) = {
    Some(
      new SierraReaderWorkerService(
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        s3client = s3Client,
        windowManager =
          new WindowManager(s3Client, bucketName, fields, resourceType),
        batchSize = batchSize,
        resourceType = resourceType,
        bucketName = bucketName,
        system = actorSystem,
        metrics = mockMetrics,
        apiUrl = apiUrl,
        sierraOauthKey = "key",
        sierraOauthSecret = "secret",
        fields = fields
      ))
  }

  it(
    "reads a window message from SQS, retrieves the bibs from Sierra and writes them to S3") {
    worker = createSierraReaderWorkerService(
      fields = "updatedDate,deletedDate,deleted,suppressed,author,title",
      batchSize = 10,
      resourceType = SierraResourceTypes.bibs
    )
    worker.get.runSQSWorker()
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    val pageNames = List("0000.json", "0001.json", "0002.json").map { label =>
      s"records_bibs/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z/$label"
    } ++ List(
      "windows_bibs_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

    eventually {
      val objects = s3Client.listObjects(bucketName).getObjectSummaries

      // there are 29 bib updates in the sierra wiremock so we expect 3 files
      objects.map { _.getKey() } shouldBe pageNames

      getRecordsFromS3(pageNames(0)) should have size 10
      getRecordsFromS3(pageNames(1)) should have size 10
      getRecordsFromS3(pageNames(2)) should have size 9

      getRecordsFromS3(pageNames(0)).head.id should startWith("b")
    }
  }

  it(
    "reads a window message from SQS, retrieves the items from Sierra and writes them to S3") {
    worker = createSierraReaderWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields",
      batchSize = 50,
      resourceType = SierraResourceTypes.items
    )
    worker.get.runSQSWorker()
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    val pageNames = List("0000.json", "0001.json", "0002.json", "0003.json")
      .map { label =>
        s"records_items/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z/$label"
      } ++ List(
      "windows_items_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

    eventually {
      val objects = s3Client.listObjects(bucketName).getObjectSummaries

      // There are 157 item records in the Sierra wiremock so we expect 4 files
      objects.map { _.getKey() } shouldBe pageNames

      getRecordsFromS3(pageNames(0)) should have size 50
      getRecordsFromS3(pageNames(1)) should have size 50
      getRecordsFromS3(pageNames(2)) should have size 50
      getRecordsFromS3(pageNames(3)) should have size 7

      getRecordsFromS3(pageNames(0)).head.id should startWith("i")
    }
  }

  it("resumes a window if it finds an in-progress set of records") {
    worker = createSierraReaderWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields",
      batchSize = 50,
      resourceType = SierraResourceTypes.items
    )

    val prefix = "records_items/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z"

    // First we pre-populate S3 with files as if they'd come from a prior run of the reader.
    s3Client.putObject(bucketName, s"$prefix/0000.json", "[]")
    s3Client.putObject(
      bucketName,
      s"$prefix/0001.json",
      """
        |[
        |  {
        |    "id": "i1794165",
        |    "modifiedDate": "2013-12-10T17:16:35Z",
        |    "data": "{}"
        |  }
        |]
      """.stripMargin
    )

    // Then we trigger the reader, and we expect it to fill in the rest.
    worker.get.runSQSWorker()
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    val pageNames = List("0000.json", "0001.json", "0002.json", "0003.json")
      .map { label =>
        s"$prefix/$label"
      } ++ List(
      "windows_items_complete/2013-12-10T17-16-35Z__2013-12-13T21-34-35Z")

    eventually {
      val objects = s3Client.listObjects(bucketName).getObjectSummaries

      // There are 157 item records in the Sierra wiremock so we expect 4 files
      objects.map { _.getKey() } shouldBe pageNames

      // These two files were pre-populated -- we check the reader hasn't overwritten these
      getRecordsFromS3(pageNames(0)) should have size 0
      getRecordsFromS3(pageNames(1)) should have size 1

      // We check the reader has filled these in correctly
      getRecordsFromS3(pageNames(2)) should have size 50
      getRecordsFromS3(pageNames(3)) should have size 7
    }
  }

  private def getRecordsFromS3(key: String): List[SierraRecord] =
    fromJson[List[SierraRecord]](getContentFromS3(bucketName, key)).get

  it(
    "returns a GracefulFailureException if it receives a message that doesn't contain start or end values") {
    worker = createSierraReaderWorkerService(fields = "")

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldBe a[GracefulFailureException]
    }

  }

  it(
    "does not return a GracefulFailureException if it cannot reach the Sierra API") {
    worker = createSierraReaderWorkerService(
      fields = "",
      apiUrl = "http://localhost:5050"
    )

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")

    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldNot be(a[GracefulFailureException])
    }
  }

  private def stopWorker(worker: Option[SierraReaderWorkerService]) = {
    eventually {
      worker.fold(true)(_.cancelRun()) shouldBe true
    }
  }
}
