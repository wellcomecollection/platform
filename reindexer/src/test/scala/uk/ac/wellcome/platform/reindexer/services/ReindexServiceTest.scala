package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience {

  def createReindexService =
    new ReindexService(
      dynamoDbClient,
      Map("reindex" -> DynamoConfig("applicationName", "streamArn", reindexTableName)))

  it(
    "should return a list of indexes with their current and requested versions") {
    val expectedReindexList = List(
      Reindex("foo", 2, 1),
      Reindex("bar", 1, 1)
    )

    expectedReindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = createReindexService

    val op = reindexService.getIndices

    whenReady(op) { indices =>
      expectedReindexList should contain theSameElementsAs indices
    }
  }

  it(
    "should return a list of indexes in need of reindexing"
  ) {
    val reindexList = List(
      Reindex("foo", 2, 1),
      Reindex("bar", 1, 1)
    )

    val expectedReindexList = List(
      Reindex("foo", 2, 1)
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = createReindexService

    val op = reindexService.getIndicesForReindex

    whenReady(op) { indices =>
      expectedReindexList should contain theSameElementsAs indices
    }
  }

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

  it("should return the rows of a table with an 'old' reindex version") {

    val currentVersion = 1
    val requestedVersion = 2

    val outOfdateCalmTransformableList = List(
      CalmTransformable(
        RecordID = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}""",
        ReindexVersion = 1
      ))

    val inDateCalmTransferrableList = List(
      CalmTransformable(
        RecordID = "RecordID2",
        RecordType = "Collection",
        AltRefNo = "AltRefNo2",
        RefNo = "RefNo2",
        data = """{"AccessStatus": ["public"]}""",
        ReindexVersion = 2
      ))

    val calmTransformableList = outOfdateCalmTransformableList ++ inDateCalmTransferrableList

    val expectedCalmTransformableList =
      outOfdateCalmTransformableList.map(Right(_))

    val reindex = Reindex(calmDataTableName, requestedVersion, currentVersion)

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(calmDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexService = createReindexService

    whenReady(reindexService.getRowsWithOldReindexVersion(reindex)) { rows =>
      rows shouldBe expectedCalmTransformableList
    }
  }
}
