package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.ParallelScanner
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.ReindexableTable
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class RecordReaderTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ReindexableTable
    with IntegrationPatience {

  val exampleRecord = HybridRecord(
    id = "id",
    version = 1,
    location = ObjectLocation(
      namespace = "s3://example-bukkit",
      key = "key.json.gz"
    )
  )

  it("finds records in the table") {
    withLocalDynamoDbTable { table =>
      withRecordReader(table) { reader =>
        val records = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        val reindexJob = ReindexJob(segment = 0, totalSegments = 1)

        val recordList = records

        recordList.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        whenReady(reader.findRecordsForReindexing(reindexJob)) {
          actualRecords =>
            actualRecords should contain theSameElementsAs records
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    val table = Table("does-not-exist", "no-such-index")
    withRecordReader(table) { reader =>
      val future = reader.findRecordsForReindexing(
        ReindexJob(segment = 5, totalSegments = 10))
      whenReady(future.failed) {
        _ shouldBe a[ResourceNotFoundException]
      }
    }
  }

  private def withRecordReader[R](table: Table)(
    testWith: TestWith[RecordReader, R]): R = {

    val dynamoConfig = DynamoConfig(
      table = table.name,
      index = table.index
    )

    val parallelScanner = new ParallelScanner(
      dynamoDBClient = dynamoDbClient,
      dynamoConfig = dynamoConfig
    )

    val reader = new RecordReader(
      parallelScanner = parallelScanner
    )

    testWith(reader)
  }
}
