package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{MiroTransformable, Reindex}
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}


class CalmReindexTargetServiceTest
  extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with ExtendedPatience {

  it("should return the rows of a table with an 'old' reindex version for miro data") {

    val currentVersion = 1
    val requestedVersion = 2

    val outOfdateMiroTransformableList = List(
      MiroTransformable(
        MiroID = "Image1",
        MiroCollection = "Images-A",
        data = s"""{"image_title": "title"}""",
        ReindexVersion = 1
      )
    )

    val inDateMiroTransferrableList = List(
      MiroTransformable(
        MiroID = "Image2",
        MiroCollection = "Images-A",
        data = s"""{"image_title": "title"}""",
        ReindexVersion = 2
      )
    )

    val calmTransformableList = outOfdateMiroTransformableList ++ inDateMiroTransferrableList

    val expectedMiroTransformableList =
      outOfdateMiroTransformableList.map(Right(_))

    val reindex = Reindex(miroDataTableName, requestedVersion, currentVersion)

    calmTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexTargetService = new MiroReindexTargetService(dynamoDbClient, "MiroData")

    whenReady(reindexTargetService.getRowsWithOldReindexVersion(reindex)) { rows =>
      rows shouldBe expectedMiroTransformableList
    }
  }
}
