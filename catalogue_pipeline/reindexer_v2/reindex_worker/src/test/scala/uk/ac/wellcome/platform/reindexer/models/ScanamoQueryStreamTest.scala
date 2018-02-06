package uk.ac.wellcome.reindexer.models

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.test.utils.DynamoConstants._

import scala.collection.mutable.ListBuffer
import com.gu.scanamo.syntax._
import uk.ac.wellcome.locals.DynamoDBLocal

class ScanamoQueryStreamTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with DynamoDBLocal[MiroTransformable]
    with Matchers {

  override lazy val tableName = "scanamo-query-stream-test-table"
  val bigString = "_" * maxDynamoItemSizeinKb

  override lazy val evidence: DynamoFormat[MiroTransformable] =
    DynamoFormat[MiroTransformable]

  it("should run a given function for each batch from the query provided") {
    type ResultGroup = List[Either[DynamoReadError, MiroTransformable]]
    val resultGroups: ListBuffer[ResultGroup] = new ListBuffer[ResultGroup]()

    val expectedBatchCount = 4

    val itemsToPut = (1 to expectedBatchCount).map { i =>
      MiroTransformable(
        sourceId = s"Image$i",
        MiroCollection = "CollectionV",
        data = bigString,
        reindexVersion = 1
      )
    }.toList

    itemsToPut.foreach { item =>
      Scanamo.put(dynamoDbClient)(tableName)(item)
    }

    val expectedItemsStored = itemsToPut.map(Right(_))

    val scanamoQueryRequest = ScanamoQueryRequest(
      tableName,
      Some("ReindexTracker"),
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, 10)
        )),
      ScanamoQueryOptions.default
    )

    def updateVersion(group: ResultGroup): ResultGroup = {
      resultGroups += group
      group
    }

    val ops = ScanamoQueryStream
      .run[MiroTransformable,
           List[Either[DynamoReadError, MiroTransformable]]](
        scanamoQueryRequest,
        updateVersion)

    Scanamo.exec(dynamoDbClient)(ops)

    val actualItemsStored: ResultGroup = resultGroups.toList.flatten

    resultGroups.length shouldBe expectedBatchCount
    actualItemsStored should contain theSameElementsAs expectedItemsStored
  }
}
