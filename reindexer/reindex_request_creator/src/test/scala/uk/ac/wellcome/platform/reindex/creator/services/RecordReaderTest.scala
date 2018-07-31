package uk.ac.wellcome.platform.reindex.creator.services

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.gu.scanamo.Scanamo
import javax.naming.ConfigurationException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.platform.reindex.creator.TestRecord
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

class RecordReaderTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with LocalDynamoDbVersioned
    with ExtendedPatience {

  val shardName = "shard"
  val currentVersion = 1
  val desiredVersion = 2

  val exampleRecord = TestRecord(
    id = "id",
    version = 1,
    someData = "A ghastly gharial ganking a green golem.",
    reindexShard = shardName,
    reindexVersion = currentVersion
  )

  def exampleReindexJob(table: Table) = ReindexJob(
    dynamoConfig = DynamoConfig(
      table = table.name,
      index = table.index,
    ),
    shardId = "sierra/000",
    desiredVersion = 2
  )

  it("only selects records with an out-of-date reindex version") {
    withLocalDynamoDbTable { table =>
      withReindexRecordReaderService(table) { service =>
        val newerRecord = exampleRecord.copy(
          id = "id1",
          reindexVersion = desiredVersion + 1
        )

        val olderRecord = exampleRecord.copy(
          id = "id2"
        )

        val records = List(
          newerRecord,
          olderRecord
        )

        records.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        val expectedRecordIds = List(olderRecord.id)

        val reindexJob = ReindexJob(
          dynamoConfig = DynamoConfig(
            table = table.name,
            index = table.index,
          ),
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        whenReady(service.findRecordsForReindexing(reindexJob)) {
          actualRecordIds =>
            actualRecordIds should contain theSameElementsAs expectedRecordIds
        }
      }

    }
  }

  it("sends notifications for records in the specified shard") {
    withLocalDynamoDbTable { table =>
      withReindexRecordReaderService(table) { service =>
        val inShardRecords = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        val notInShardRecords = List(
          exampleRecord.copy(id = "id3", reindexShard = "not_the_same_shard"),
          exampleRecord.copy(id = "id4", reindexShard = "not_the_same_shard")
        )

        val reindexJob = ReindexJob(
          dynamoConfig = DynamoConfig(
            table = table.name,
            index = table.index
          ),
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        val recordList = inShardRecords ++ notInShardRecords

        recordList.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        val expectedRecordIds = inShardRecords.map { record =>
          record.id
        }

        whenReady(service.findRecordsForReindexing(reindexJob)) {
          actualRecordIds =>
            actualRecordIds should contain theSameElementsAs expectedRecordIds
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    val table = Table("does-not-exist", "no-such-index")
    withReindexRecordReaderService(table) {
      service =>
        val future = service.findRecordsForReindexing(exampleReindexJob(table))
        whenReady(future.failed) {
          _ shouldBe a[ResourceNotFoundException]
        }
    }
  }

  it("returns a failed Future if you don't specify a DynamoDB index") {
    val service = new RecordReader(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = "mytable",
        maybeIndex = None
      )
    )

    val reindexJob = ReindexJob(
      dynamoConfig = DynamoConfig(table = "mytable", maybeIndex = None),
      shardId = "example/123",
      desiredVersion = 1
    )

    val future = service.findRecordsForReindexing(reindexJob)
    whenReady(future.failed) {
      _ shouldBe a[ConfigurationException]
    }
  }

  private def withReindexRecordReaderService(table: Table)(
    testWith: TestWith[RecordReader, Assertion]) = {
    val service = new RecordReader(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = table.name,
        index = table.index
      )
    )

    testWith(service)
  }
}
