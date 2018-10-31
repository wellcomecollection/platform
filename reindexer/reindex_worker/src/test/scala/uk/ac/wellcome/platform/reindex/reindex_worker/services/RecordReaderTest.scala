package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner,
  ScanSpecScanner
}
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.ReindexableTable
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  CompleteReindexJob,
  PartialReindexJob
}
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

  it("finds records in the table with a complete reindex") {
    withLocalDynamoDbTable { table =>
      withRecordReader(table) { reader =>
        val records = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        records.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        val reindexJob = CompleteReindexJob(segment = 0, totalSegments = 1)

        whenReady(reader.findRecordsForReindexing(reindexJob)) {
          actualRecords =>
            actualRecords should contain theSameElementsAs records
        }
      }
    }
  }

  it("finds records in the table with a maxResults reindex") {
    withLocalDynamoDbTable { table =>
      withRecordReader(table) { reader =>
        (1 to 15).foreach { id =>
          val record = exampleRecord.copy(id = id.toString)
          Scanamo.put(dynamoDbClient)(table.name)(record)
        }

        val reindexJob = PartialReindexJob(maxRecords = 5)

        whenReady(reader.findRecordsForReindexing(reindexJob)) {
          actualRecords => actualRecords should have size 5
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

    val scanSpecScanner = new ScanSpecScanner(dynamoDbClient)

    val parallelScanner = new ParallelScanner(
      scanSpecScanner = scanSpecScanner,
      dynamoConfig = dynamoConfig
    )

    val maxRecordsScanner = new MaxRecordsScanner(
      scanSpecScanner = scanSpecScanner,
      dynamoConfig = dynamoConfig
    )

    val reader = new RecordReader(
      maxRecordsScanner = maxRecordsScanner,
      parallelScanner = parallelScanner
    )

    testWith(reader)
  }
}
