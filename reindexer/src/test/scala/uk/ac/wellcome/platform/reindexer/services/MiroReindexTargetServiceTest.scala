package uk.ac.wellcome.platform.reindexer.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{MiroTransformable, Reindex}
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.test.utils.{
  DynamoDBLocal,
  ExtendedPatience,
  MetricsSenderLocal
}

class MiroReindexTargetServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with MetricsSenderLocal
    with ExtendedPatience {

  it("should update the correct index to the requested version") {

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

    val miroTransformableList = outOfdateMiroTransformableList ++ inDateMiroTransferrableList

    val expectedMiroTransformableList =
      outOfdateMiroTransformableList.map(_.copy(ReindexVersion = 2))

    val reindex = Reindex(miroDataTableName, requestedVersion, currentVersion)
    val reindexAttempt = ReindexAttempt(reindex)
    val expectedReindexAttempt = reindexAttempt.copy(
      reindex = reindex,
      successful = expectedMiroTransformableList,
      attempt = 1
    )

    miroTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexTargetService =
      new MiroReindexTargetService(dynamoDbClient, "MiroData", metricsSender)

    whenReady(reindexTargetService.runReindex(reindexAttempt)) {
      reindexAttempt =>
        reindexAttempt shouldBe expectedReindexAttempt
    }

  }
}
