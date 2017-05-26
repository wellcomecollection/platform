package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{CalmTransformable, MiroTransformable, Reindex}
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}

class CalmReindexTargetServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience {

  it(
    "should update the correct index to the requested version") {

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
      outOfdateCalmTransformableList.map(_.copy(ReindexVersion = 2))

    val reindex = Reindex(calmDataTableName, requestedVersion, currentVersion)

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(calmDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexTargetService =
      new CalmReindexTargetService(dynamoDbClient, "CalmData")
    val reindexAttempt = ReindexAttempt(reindex)
    val expectedReindexAttempt = reindexAttempt.copy(
      reindex = reindex,
      successful = expectedCalmTransformableList,
      attempt = 1
    )

    whenReady(reindexTargetService.runReindex(reindexAttempt)) {
      reindexAttempt =>
        reindexAttempt shouldBe expectedReindexAttempt
    }

  }
}
