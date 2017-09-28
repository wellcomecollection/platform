package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}

import scala.concurrent.Future

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience
    with MockitoSugar {

  val dynamoConfigs = Map(
    "reindex" -> DynamoConfig("applicationName",
                              "streamArn",
                              reindexTableName),
    "calm" -> DynamoConfig("applicationName", "streamArn", calmDataTableName)
  )

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

  def createReindexService =
    new ReindexService(
      new ReindexTrackerService(
        dynamoDbClient,
        dynamoConfigs,
        "CalmData",
        reindexShard
      ),
      new CalmReindexTargetService(
        dynamoDBClient = dynamoDbClient,
        targetTableName = "CalmData",
        metricsSender = metricsSender
      ),
      new MetricsSender("reindexer-tests", mock[AmazonCloudWatch])
    )

  val currentVersion = 1
  val requestedVersion = 2

  it(
    "should increment the reindexVersion to the value requested on all items of a table in need of reindex") {

    val calmTransformableList = List(
      CalmTransformable(
        RecordID = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}""",
        ReindexVersion = currentVersion
      ))

    val expectedCalmTransformableList = calmTransformableList
      .map(_.copy(ReindexVersion = requestedVersion))
      .map(Right(_))

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(calmDataTableName))

    val reindex = Reindex(calmDataTableName, reindexShard, requestedVersion, currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexService = createReindexService

    whenReady(reindexService.run) { _ =>
      Scanamo.scan[CalmTransformable](dynamoDbClient)(calmDataTableName) shouldBe expectedCalmTransformableList
      Scanamo.get[Reindex](dynamoDbClient)(reindexTableName)(
        'TableName -> "CalmData" and 'ReindexShard -> reindexShard) shouldBe Some(
        Right(reindex.copy(CurrentVersion = requestedVersion)))
    }
  }

  it("should retry while the target reports that there are not updated items") {
    val calmReindexTargetService = mock[CalmReindexTargetService]

    val reindex = Reindex(calmDataTableName, reindexShard, requestedVersion, currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val calmTransformableList = List(
      CalmTransformable(
        RecordID = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}""",
        ReindexVersion = currentVersion
      ))

    when(calmReindexTargetService.runReindex(ReindexAttempt(reindex)))
      .thenReturn(Future.successful(ReindexAttempt(reindex, true, 1)))

    (1 to 4).foreach { x =>
      when(
        calmReindexTargetService.runReindex(ReindexAttempt(reindex, true, x)))
        .thenReturn(Future.successful(ReindexAttempt(reindex, false, x + 1)))
    }

    when(
      calmReindexTargetService.runReindex(ReindexAttempt(reindex, false, 5)))
      .thenReturn(Future.successful(ReindexAttempt(reindex, true, 6)))

    val reindexService = new ReindexService(
      new ReindexTrackerService(
        dynamoDbClient,
        dynamoConfigs,
        "CalmData",
        reindexShard
      ),
      calmReindexTargetService,
      new MetricsSender("reindexer-tests", mock[AmazonCloudWatch])
    )

    whenReady(reindexService.run) { _ =>
      Scanamo.get[Reindex](dynamoDbClient)(reindexTableName)(
        'TableName -> "CalmData") shouldBe Some(
        Right(reindex.copy(CurrentVersion = requestedVersion)))
    }

  }

  it("should report successful when there is no work to do") {
    val reindex = Reindex(calmDataTableName, reindexShard, currentVersion, currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexService = createReindexService

    whenReady(reindexService.run) { _ =>
      // Future does not fail
    }
  }
}
