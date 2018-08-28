package uk.ac.wellcome.platform.reindex.creator.services

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import javax.naming.ConfigurationException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.creator.TestRecord
import uk.ac.wellcome.platform.reindex.creator.fixtures.ReindexableTable
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class RecordReaderTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ReindexableTable
    with ExtendedPatience {

  val shardId = "shard"

  val exampleRecord = TestRecord(
    id = "id",
    version = 1,
    s3key = "s3://id",
    reindexShard = shardId
  )

  it("finds records in the specified shard") {
    withLocalDynamoDbTable { table =>
      withRecordReader(table) { reader =>
        val inShardRecords = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        val notInShardRecords = List(
          exampleRecord.copy(id = "id3", reindexShard = "not_the_same_shard"),
          exampleRecord.copy(id = "id4", reindexShard = "not_the_same_shard")
        )

        val reindexJob = ReindexJob(shardId = shardId)

        val recordList = inShardRecords ++ notInShardRecords

        val expectedRecords = inShardRecords.map { testRecord =>
          HybridRecord(
            id = testRecord.id,
            version = testRecord.version,
            s3key = testRecord.s3key
          )
        }

        recordList.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        whenReady(reader.findRecordsForReindexing(reindexJob)) {
          actualRecords =>
            actualRecords should contain theSameElementsAs expectedRecords
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    val table = Table("does-not-exist", "no-such-index")
    withRecordReader(table) { reader =>
      val future = reader.findRecordsForReindexing(
        ReindexJob(shardId = shardId))
      whenReady(future.failed) {
        _ shouldBe a[ResourceNotFoundException]
      }
    }
  }

  it("fails upon construction if you don't specify a DynamoDB index") {
    val dynamoConfig = DynamoConfig(
      table = "mytable",
      maybeIndex = None
    )

    intercept[ConfigurationException] {
      new RecordReader(
        dynamoDbClient = dynamoDbClient,
        dynamoConfig = dynamoConfig
      )
    }
  }

  private def withRecordReader[R](table: Table)(
    testWith: TestWith[RecordReader, R]): R = {
    val dynamoConfig = DynamoConfig(
      table = table.name,
      index = table.index
    )

    val reader = new RecordReader(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = dynamoConfig
    )

    testWith(reader)
  }
}
