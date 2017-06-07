package uk.ac.wellcome.utils

import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.MiroTransformable
import uk.ac.wellcome.test.utils.DynamoDBLocal

import scala.collection.mutable.ListBuffer

class ScanamoQueryStreamTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with DynamoDBLocal
    with Matchers {

  final val maxDynamoItemSizeinKb = 400000
  final val maxDynamoQueryResultSizeInKb = 1000000

  val bigString = "_" * maxDynamoItemSizeinKb

  it("run a given function for each batch from the query provided") {

    type ResultGroup = List[Either[DynamoReadError, MiroTransformable]]
    var resultGroups: ListBuffer[ResultGroup] = new ListBuffer[ResultGroup]()

    val expectedBatchCount = 4
    val numberofRequiredPuts = (expectedBatchCount * maxDynamoQueryResultSizeInKb) / maxDynamoItemSizeinKb

    val itemsToPut = (1 to numberofRequiredPuts)
      .map(
        i =>
          MiroTransformable(
            MiroID = s"Image$i",
            MiroCollection = "Collection",
            data = bigString,
            ReindexVersion = 1
        ))

    itemsToPut.par.foreach(
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

    def updateVersion(
      resultGroup: List[Either[DynamoReadError, MiroTransformable]])
      : List[Either[DynamoReadError, MiroTransformable]] = {

      resultGroups += resultGroup

      resultGroup
    }

    val currentState =
      Scanamo.scan[MiroTransformable](dynamoDbClient)("MiroData")

    val ops = ScanamoQueryStream
      .run[MiroTransformable, Either[DynamoReadError, MiroTransformable]](
        scanamoQueryRequest,
        updateVersion)

    Scanamo.exec(dynamoDbClient)(ops)

    val actualItemsStored: ResultGroup = resultGroups.toList.flatten

    resultGroups.length shouldBe expectedBatchCount
    actualItemsStored should contain theSameElementsAs expectedItemsStored
  }
}
