package uk.ac.wellcome.reindexer.models

import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.DynamoConstants._

import scala.collection.mutable.ListBuffer

import com.gu.scanamo.syntax._

class ScanamoQueryStreamTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with DynamoDBLocal
    with Matchers {

  val bigString = "_" * maxDynamoItemSizeinKb

  it("should run a given function for each batch from the query provided") {

    type ResultGroup = List[Either[DynamoReadError, MiroTransformable]]
    val resultGroups: ListBuffer[ResultGroup] = new ListBuffer[ResultGroup]()

    val expectedBatchCount = 4
    val itemsToPut = generateMiroTransformablesInBatches(expectedBatchCount)

    itemsToPut.foreach(
      Scanamo.put(dynamoDbClient)(miroDataTableName)
    )

    val expectedItemsStored = itemsToPut.map(Right(_))

    val scanamoQueryRequest = ScanamoQueryRequest(
      miroDataTableName,
      Some("ReindexTracker"),
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, 10)
        )),
      ScanamoQueryOptions.default
    )

    def updateVersion(resultGroup: ResultGroup): ResultGroup = {
      resultGroups += resultGroup

      resultGroup
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
