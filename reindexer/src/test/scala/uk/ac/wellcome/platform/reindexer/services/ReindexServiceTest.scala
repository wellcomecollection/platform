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
    new CalmReindexService(
      dynamoDbClient,
      Map(
        "reindex" -> DynamoConfig("applicationName",
                                         "streamArn",
                                         reindexTableName),
        "calm" -> DynamoConfig("applicationName",
                                   "streamArn",
                                   calmDataTableName)
      ),
      "CalmData"
    )

  it("should return the correct index with current and requested versions") {
    val expectedReindex = Reindex("CalmData", 2, 1)
    val reindexList = List(
      expectedReindex,
      Reindex("MiroData", 1, 1)
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = createReindexService

    val op = reindexService.getIndices

    whenReady(op) { reindex =>
      expectedReindex shouldBe reindex
    }
  }

  it(
    "should return the index in need of reindexing"
  ) {
    val expectedReindex = Reindex("CalmData", 2, 1)
    val reindexList = List(
      expectedReindex,
      Reindex("MiroData", 1, 1)
    )

    val expectedReindexList = List(
      Reindex("foo", 2, 1)
    )

    reindexList.foreach(Scanamo.put(dynamoDbClient)(reindexTableName))

    val reindexService = createReindexService

    val op = reindexService.getIndicesForReindex

    whenReady(op) { reindex =>
      Some(expectedReindex) shouldBe reindex
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
