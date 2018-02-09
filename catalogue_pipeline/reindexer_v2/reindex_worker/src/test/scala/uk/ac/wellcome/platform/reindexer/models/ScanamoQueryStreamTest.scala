package uk.ac.wellcome.platform.reindexer.models

import shapeless.ops.hlist._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.test.utils.DynamoConstants._

import scala.collection.mutable.ListBuffer
import com.gu.scanamo.syntax._
import shapeless.{HList, LabelledGeneric, Witness}
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.{Versioned, VersionedDynamoFormatWrapper}
import uk.ac.wellcome.storage.HybridRecord
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

class ScanamoQueryStreamTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with DynamoDBLocal[HybridRecord]
    with Matchers {

  override lazy val tableName = "scanamo-query-stream-test-table"
  val bigString = "_" * maxDynamoItemSizeinbytes

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Versioned
    .toVersionedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  it("should run a given function for each batch from the query provided") {
    type ResultGroup = List[Either[DynamoReadError, HybridRecord]]
    val resultGroups: ListBuffer[ResultGroup] = new ListBuffer[ResultGroup]()

    val itemsToPut = (1 to 10).map { i =>
      HybridRecord(
        sourceId = s"Image$i",
        sourceName = "some-source",
        version = 1,
        reindexShard = "default",
        reindexVersion = 1,
        s3key = bigString
      )
    }.toList

    val totalNumberOfBytes = itemsToPut.foldLeft(0)((acc, hybridRecord) => {
      val recordSizeInBytes = calculateRecordSize(hybridRecord)
      acc + recordSizeInBytes
    })

    val expectedBatchCount = totalNumberOfBytes/maxDynamoQueryResultSizeInBytes

    itemsToPut.foreach { item =>
      Scanamo.put(dynamoDbClient)(tableName)(item)(enrichedDynamoFormat)
    }

    val expectedItemsStored = itemsToPut.map(Right(_))

    val scanamoQueryRequest = ScanamoQueryRequest(
      tableName,
      Some(indexName),
      Query(
        AndQueryCondition(
          KeyEquals('reindexShard, "default"),
          KeyIs('reindexVersion, LT, 10)
        )),
      ScanamoQueryOptions.default
    )

    def updateVersion(group: ResultGroup): ResultGroup = {
      resultGroups += group
      group
    }

    val ops = ScanamoQueryStream
      .run[HybridRecord, List[Either[DynamoReadError, HybridRecord]]](
        scanamoQueryRequest,
        updateVersion)

    Scanamo.exec(dynamoDbClient)(ops)

    val actualItemsStored: ResultGroup = resultGroups.toList.flatten

    actualItemsStored should contain theSameElementsAs expectedItemsStored

    resultGroups.length shouldBe expectedBatchCount
  }

  def calculateRecordSize[R <: HList](hybridRecord: HybridRecord)(implicit versionedDynamoFormatWrapper: VersionedDynamoFormatWrapper[HybridRecord]): Int  = {
    val attributeValue = versionedDynamoFormatWrapper.enrichedDynamoFormat.write(hybridRecord)
    AttributeValueSizeCalculator.calculateAttributeSizeInBytes(attributeValue)
  }


}
