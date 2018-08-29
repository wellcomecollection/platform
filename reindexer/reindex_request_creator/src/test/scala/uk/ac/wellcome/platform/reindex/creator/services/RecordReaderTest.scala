package uk.ac.wellcome.platform.reindex.creator.services

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import javax.naming.ConfigurationException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.creator.TestRecord
import uk.ac.wellcome.platform.reindex.creator.fixtures.{
  ReindexFixtures,
  ReindexableTable
}
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
    with ExtendedPatience
    with ReindexFixtures {

  val shardName = "shard"

  val exampleRecord = TestRecord(
    id = "id",
    version = 1,
    s3key = "s3://id",
    reindexShard = shardName
  )

  it("finds records in the specified shard") {
    withLocalDynamoDbTable { table =>
      withRecordReader { reader =>
        val inShardRecords = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        val notInShardRecords = List(
          exampleRecord.copy(id = "id3", reindexShard = "not_the_same_shard"),
          exampleRecord.copy(id = "id4", reindexShard = "not_the_same_shard")
        )

        val reindexJob = createReindexJobWith(
          table = table,
          shardId = shardName
        )

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
    withRecordReader { reader =>
      val future = reader.findRecordsForReindexing(
        createReindexJobWith(Table("does-not-exist", "no-such-index")))
      whenReady(future.failed) {
        _ shouldBe a[ResourceNotFoundException]
      }
    }
  }

  it("returns a failed Future if you don't specify a DynamoDB index") {
    val reader = new RecordReader(dynamoDbClient = dynamoDbClient)

    val reindexJob = createReindexJobWith(
      dynamoConfig = DynamoConfig(
        table = "mytable",
        maybeIndex = None
      )
    )

    val future = reader.findRecordsForReindexing(reindexJob)
    whenReady(future.failed) {
      _ shouldBe a[ConfigurationException]
    }
  }

  private def withRecordReader(
    testWith: TestWith[RecordReader, Assertion]) = {
    val service = new RecordReader(dynamoDbClient = dynamoDbClient)

    testWith(service)
  }
}
