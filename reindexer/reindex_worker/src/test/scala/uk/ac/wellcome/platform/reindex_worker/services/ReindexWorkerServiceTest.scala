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
    with DynamoDBLocal[HybridRecord]
    with MockitoSugar
    with SNSLocal
    with SQSLocal
    with ScalaFutures {

  val actorSystem = ActorSystem()

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests",
                      mock[AmazonCloudWatch],
                      actorSystem)

  override lazy val tableName = "reindex-worker-service-test"

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Sourced
    .toSourcedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  val queueUrl = createQueueAndReturnUrl("reindex-worker-service-test-q")
  val topicArn = createTopicAndReturnArn("reindex-worker-service-test-topic")

  it("returns a successful Future if the reindex completes correctly") {
    val reindexJob = ReindexJob(
      shardId = "sierra/123",
      desiredVersion = 6
    )

    val hybridRecord = HybridRecord(
      version = 1,
      sourceId = "sierra",
      sourceName = "111",
      s3key = "s3://reindexWST/example.json",
      reindexShard = reindexJob.shardId,
      reindexVersion = reindexJob.desiredVersion - 1
    )

    Scanamo.put(dynamoDbClient)(tableName)(hybridRecord)(enrichedDynamoFormat)

    val expectedRecords = List(
      hybridRecord.copy(version = hybridRecord.version + 1,
                        reindexVersion = reindexJob.desiredVersion)
    )

    val invalidSqsMessage = SQSMessage(
      subject = None,
      body = toJson(reindexJob).get,
      topic = "topic",
      messageType = "message",
      timestamp = "now"
    )

    val service = reindexWorkerService()

    val future = service.processMessage(message = invalidSqsMessage)

    whenReady(future) { _ =>
      val actualRecords: List[HybridRecord] =
        Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

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
    val reindexJob = ReindexJob(
      shardId = "sierra/333",
      desiredVersion = 3
    )

    val sqsMessage = SQSMessage(
      subject = None,
      body = toJson(reindexJob).get,
      topic = "topic",
      messageType = "message",
      timestamp = "now"
    )

    val service = reindexWorkerService(dynamoTableName = "does-not-exist")

    val future = service.processMessage(message = sqsMessage)

    whenReady(future.failed) { result =>
      result shouldBe a[GracefulFailureException]
      result.getMessage should include(
        "Cannot do operations on a non-existent table")
    }
  }

  it("returns a failed Future if the reindex job fails after a delay") {
    val targetService = mock[ReindexService]
    when(
      targetService.runReindex(any[ReindexJob])(
        any[SourcedDynamoFormatWrapper[HybridRecord]]))
      .thenReturn(Future {
        Thread.sleep(500)
        throw new RuntimeException("This took too long")
      })

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

    whenReady(future.failed) { result =>
      result shouldBe a[GracefulFailureException]
      result.getMessage should include("This took too long")
    }
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
