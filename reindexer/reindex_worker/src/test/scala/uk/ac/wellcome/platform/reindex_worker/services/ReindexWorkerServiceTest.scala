package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{Sourced, SourcedDynamoFormatWrapper}
import uk.ac.wellcome.models.aws.{
  DynamoConfig,
  SNSConfig,
  SQSConfig,
  SQSMessage
}
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.{SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ReindexWorkerServiceTest
    extends FunSpec
    with Matchers
    with DynamoDBLocal[TestRecord]
    with MockitoSugar
    with SNSLocal
    with SQSLocal
    with ScalaFutures {

  val actorSystem = ActorSystem()

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      mock[AmazonCloudWatch],
      actorSystem)

  override lazy val tableName = "table"

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

  val queueUrl = createQueueAndReturnUrl("reindex-worker-service-test-q")
  val topicArn = createTopicAndReturnArn("reindex-worker-service-test-topic")

  it("returns a successful Future if the reindex completes correctly") {
    val reindexJob = ReindexJob(
      shardId = "sierra/123",
      desiredVersion = 6
    )

    val testRecord = TestRecord(
      id = "id/111",
      version = 1,
      someData = "A dire daliance directly dancing due down.",
      reindexShard = reindexJob.shardId,
      reindexVersion = reindexJob.desiredVersion - 1
    )

    Scanamo.put(dynamoDbClient)(tableName)(testRecord)

    val expectedRecords = List(
      testRecord.copy(
        version = testRecord.version + 1,
        reindexVersion = reindexJob.desiredVersion
      )
    )

    val sqsMessage = SQSMessage(
      subject = None,
      body = toJson(reindexJob).get,
      topic = "topic",
      messageType = "message",
      timestamp = "now"
    )

    val service = reindexWorkerService()

    val future = service.processMessage(message = sqsMessage)

    whenReady(future) { _ =>
      val actualRecords: List[TestRecord] =
        Scanamo.scan[TestRecord](dynamoDbClient)(tableName).map(_.right.get)

      actualRecords shouldBe expectedRecords
    }
  }

  it(
    "returns a failed Future if it cannot parse the SQS message as a ReindexJob") {
    val sqsMessage = SQSMessage(
      subject = None,
      body = "<xml>What is JSON.</xl?>",
      topic = "topic",
      messageType = "message",
      timestamp = "now"
    )

    val service = reindexWorkerService()

    val future = service.processMessage(message = sqsMessage)

    whenReady(future.failed) { result =>
      result shouldBe a[GracefulFailureException]
      result.getMessage should include(
        "expected json value got < (line 1, column 1)")
    }
  }

  it("returns a failed Future if the reindex job fails") {
    val exception = new RuntimeException(
      "Flobberworm! Fickle failure frustrates my fortunes!")

    val targetService = mock[ReindexService]
    when(targetService.runReindex(any[ReindexJob]))
      .thenReturn(Future { throw exception })

    val service = new ReindexWorkerService(
      targetService = targetService,
      reader = mock[SQSReader],
      snsWriter = mock[SNSWriter],
      system = actorSystem,
      metrics = metricsSender
    )

    val reindexJob = ReindexJob(
      shardId = "sierra/444",
      desiredVersion = 4
    )

    val sqsMessage = SQSMessage(
      subject = None,
      body = toJson(reindexJob).get,
      topic = "topic",
      messageType = "message",
      timestamp = "now"
    )

    val future = service.processMessage(message = sqsMessage)

    whenReady(future.failed) { _ shouldBe exception }
  }

  private def reindexWorkerService(
    dynamoTableName: String = tableName): ReindexWorkerService = {
    new ReindexWorkerService(
      targetService = new ReindexService(
        dynamoDBClient = dynamoDbClient,
        metricsSender = metricsSender,
        versionedDao = new VersionedDao(
          dynamoDbClient = dynamoDbClient,
          dynamoConfig = DynamoConfig(table = dynamoTableName)
        ),
        dynamoConfig = DynamoConfig(table = dynamoTableName)
      ),
      reader = new SQSReader(
        sqsClient = sqsClient,
        sqsConfig = SQSConfig(
          queueUrl = queueUrl,
          waitTime = 1 second,
          maxMessages = 1
        )
      ),
      snsWriter = new SNSWriter(
        snsClient = snsClient,
        snsConfig = SNSConfig(topicArn = topicArn)
      ),
      system = actorSystem,
      metrics = metricsSender
    )
  }
}
