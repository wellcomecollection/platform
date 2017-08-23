package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.test.utils.{DynamoDBLocal, ExtendedPatience}

class MiroReindexTargetServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with MockitoSugar
    with ExtendedPatience {

  it("should update the correct index to the requested version") {

    val currentVersion = 1
    val requestedVersion = 3

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
        ReindexVersion = requestedVersion
      )
    )

    val miroTransformableList = outOfdateMiroTransformableList ++ inDateMiroTransferrableList

    val reindex = Reindex(miroDataTableName, requestedVersion, currentVersion)
    val reindexAttempt = ReindexAttempt(reindex)
    val expectedReindexAttempt = reindexAttempt.copy(
      reindex = reindex,
      successful = true,
      attempt = 1
    )

    miroTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val metricsSender: MetricsSender =
      new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

    val reindexTargetService =
      new MiroReindexTargetService(dynamoDbClient, "MiroData", metricsSender)

    whenReady(reindexTargetService.runReindex(reindexAttempt)) {
      reindexAttempt =>
        reindexAttempt shouldBe expectedReindexAttempt
        Scanamo
          .scan[MiroTransformable](dynamoDbClient)(miroDataTableName)
          .map {
            case Right(miroTranformable) => miroTranformable.ReindexVersion
          } should contain only requestedVersion
    }

  }
}
