package uk.ac.wellcome.platform.reindex_worker.services

import javax.naming.ConfigurationException

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

class ReindexServiceTest
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

  val exampleReindexJob = ReindexJob(
    shardId = "sierra/000",
    desiredVersion = 2
  )

  private def withReindexService(table: Table)(
    testWith: TestWith[ReindexService, Assertion]) = {
    val reindexService = new ReindexService(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = table.name,
        index = table.index
      )
    )

    testWith(reindexService)
  }

  it("only updates records with a lower than desired reindexVersion") {
    withLocalDynamoDbTable { table =>
      withReindexService(table) { reindexService =>
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

        val expectedRecords = List(
          newerRecord,
          olderRecord.copy(
            reindexVersion = desiredVersion,
            version = 2
          )
        )

        records.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        val reindexJob = ReindexJob(
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        whenReady(reindexService.runReindex(reindexJob)) { _ =>
          val records = Scanamo
            .scan[TestRecord](dynamoDbClient)(table.name)
            .map(_.right.get)

          records should contain theSameElementsAs expectedRecords
        }
      }
    }
  }

  it("updates records in the specified shard") {
    withLocalDynamoDbTable { table =>
      withReindexService(table) { reindexService =>
        val inShardRecords = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        val notInShardRecords = List(
          exampleRecord.copy(id = "id3", reindexShard = "not_the_same_shard"),
          exampleRecord.copy(id = "id4", reindexShard = "not_the_same_shard")
        )

        val reindexJob = ReindexJob(
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        val recordList = inShardRecords ++ notInShardRecords

        recordList.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        val expectedUpdatedRecords = inShardRecords.map(
          record =>
            record
              .copy(
                reindexVersion = desiredVersion,
                version = record.version + 1))

        whenReady(reindexService.runReindex(reindexJob)) { _ =>
          val testRecords =
            Scanamo
              .scan[TestRecord](dynamoDbClient)(table.name)
              .map(_.right.get)

          testRecords.filter(_.reindexShard != shardName) should contain theSameElementsAs notInShardRecords
          testRecords.filter(_.reindexShard == shardName) should contain theSameElementsAs expectedUpdatedRecords
        }
      }
    }
  }

  it("returns a failed Future if the reindex is only partially successful") {
    withLocalDynamoDbTable { table =>
      withReindexService(table) { reindexService =>
        val inShardRecords = List(
          exampleRecord.copy(id = "id1"),
          exampleRecord.copy(id = "id2")
        )

        inShardRecords.foreach(record =>
          Scanamo.put(dynamoDbClient)(table.name)(record))

        // This record doesn't conform to our ReindexRecord type (it doesn't
        // have a version field), but it is in the same reindex shard as
        // the other two records -- so it will be picked up for reindexing,
        // but won't succeed.
        case class BadRecord(
          id: String,
          reindexShard: String,
          reindexVersion: Int
        )

        Scanamo.put(dynamoDbClient)(table.name)(
          BadRecord(
            id = "badId1",
            reindexShard = shardName,
            reindexVersion = currentVersion
          ))

        val reindexJob = ReindexJob(
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        val future = reindexService.runReindex(reindexJob)
        whenReady(future.failed) {
          _ shouldBe a[GracefulFailureException]
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    withReindexService(Table("does-not-exist", "no-such-index")) { service =>
      val future = service.runReindex(exampleReindexJob)
      whenReady(future.failed) {
        _ shouldBe a[ResourceNotFoundException]
      }
    }
  }

  it("returns a failed Future if you don't specify a DynamoDB index") {
    val service = new ReindexService(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = "mytable",
        maybeIndex = None
      )
    )

    val future = service.runReindex(exampleReindexJob)
    whenReady(future.failed) {
      _ shouldBe a[ConfigurationException]
    }
  }

}
