package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.test.utils.ExtendedPatience

class MiroReindexTargetServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal
    with MockitoSugar
    with ExtendedPatience {

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

  it("should only update images in the specified ReindexShard") {
    val currentVersion = 1
    val requestedVersion = 2

    val inShardMiroTransformables = List(
      MiroTransformable(
        sourceId = "Image_A1",
        MiroCollection = "Images-A",
        data = s""""{"image_title": "An almanac about armadillos"}""",
        ReindexShard = "Images-A",
        ReindexVersion = currentVersion
      ),
      MiroTransformable(
        sourceId = "Image_A2",
        MiroCollection = "Images-A",
        data = s""""{"image_title": "Asking after an aardvark"}""",
        ReindexShard = "Images-A",
        ReindexVersion = currentVersion
      )
    )

    val diffShardMiroTransformables = List(
      MiroTransformable(
        sourceId = "Image_B1",
        MiroCollection = "Images-B",
        data = s""""{"image_title": "Buying books about beavers"}""",
        ReindexShard = "Images-B",
        ReindexVersion = currentVersion
      ),
      MiroTransformable(
        sourceId = "Image_C1",
        MiroCollection = "Images-C",
        data = s""""{"image_title": "Calling a crafty caterpillar"}""",
        ReindexShard = "Images-C",
        ReindexVersion = currentVersion
      )
    )

    val miroTransformableList = inShardMiroTransformables ++ diffShardMiroTransformables

    val reindex = Reindex(miroDataTableName,
                          reindexShard,
                          requestedVersion,
                          currentVersion)
    val reindexAttempt = ReindexAttempt(reindex)
    val expectedReindexAttempt = reindexAttempt.copy(
      reindex = reindex,
      successful = true,
      attempt = 1
    )

    miroTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexTargetService =
      new ReindexTargetService[MiroTransformable](
        dynamoDBClient = dynamoDbClient,
        targetTableName = "MiroData",
        targetReindexShard = inShardMiroTransformables.head.ReindexShard,
        metricsSender = metricsSender
      )

    whenReady(reindexTargetService.runReindex(reindexAttempt)) {
      reindexAttempt =>
        reindexAttempt shouldBe expectedReindexAttempt
        val reindexVersions = Scanamo
          .scan[MiroTransformable](dynamoDbClient)(miroDataTableName)
          .map {
            case Right(miroTranformable) => miroTranformable.ReindexVersion
          }

        reindexVersions.filter { _ == currentVersion }.length shouldBe diffShardMiroTransformables.length
        reindexVersions.filter { _ == requestedVersion }.length shouldBe inShardMiroTransformables.length
    }

  }

  it("should update the correct index to the requested version") {

    val currentVersion = 1
    val requestedVersion = 3

    val outOfdateMiroTransformableList = List(
      MiroTransformable(
        sourceId = "Image1",
        MiroCollection = "Images-A",
        data = s"""{"image_title": "title"}""",
        ReindexVersion = currentVersion
      )
    )

    val inDateMiroTransferrableList = List(
      MiroTransformable(
        sourceId = "Image2",
        MiroCollection = "Images-A",
        data = s"""{"image_title": "title"}""",
        ReindexVersion = requestedVersion
      )
    )

    val miroTransformableList = outOfdateMiroTransformableList ++ inDateMiroTransferrableList

    val reindex = Reindex(miroDataTableName,
                          reindexShard,
                          requestedVersion,
                          currentVersion)

    val reindexAttempt = ReindexAttempt(reindex)
    val expectedReindexAttempt = reindexAttempt.copy(
      reindex = reindex,
      successful = true,
      attempt = 1
    )

    miroTransformableList.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName))

    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    val reindexTargetService =
      new ReindexTargetService[MiroTransformable](
        dynamoDBClient = dynamoDbClient,
        targetTableName = "MiroData",
        metricsSender = metricsSender
      )

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
