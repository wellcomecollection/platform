package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience, MetricsSenderLocal}

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MetricsSenderLocal
    with DynamoDBLocal
    with ExtendedPatience {

  val dynamoConfigs = Map(
    "reindex" -> DynamoConfig("applicationName",
                              "streamArn",
                              reindexTableName),
    "calm" -> DynamoConfig("applicationName", "streamArn", calmDataTableName)
  )

  def createReindexService =
    new ReindexService(
      new ReindexTrackerService(
        dynamoDbClient,
        dynamoConfigs,
        "CalmData"
      ),
      new CalmReindexTargetService(dynamoDbClient, "CalmData", metricsSender),
      metricsSender
    )

  it(
    "should increment the reindexVersion to the value requested on all items of a table in need of reindex"
  ) {

    val currentVersion = 1
    val requestedVersion = 2

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

    val reindexList = List(
      Reindex(calmDataTableName, requestedVersion, currentVersion)
    )

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(calmDataTableName))

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = createReindexService

    whenReady(reindexService.run) { _ =>
      Scanamo.scan[CalmTransformable](dynamoDbClient)(calmDataTableName) shouldBe expectedCalmTransformableList
    }
  }
}
