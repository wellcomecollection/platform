package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.test.fixtures.{AkkaFixtures, LocalDynamoDb, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import scala.concurrent.duration._

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with AkkaFixtures
    with LocalDynamoDb[TestRecord]
    with MockitoSugar
    with ExtendedPatience {

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

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

  private def withReindexService(tableName: String, indexName: String)(
    testWith: TestWith[ReindexService, Assertion]) = {
      withActorSystem { actorSystem =>

        val metricsSender = new MetricsSender(
          namespace = "reindex-service-test",
          flushInterval = 100 milliseconds,
          amazonCloudWatch = mock[AmazonCloudWatch],
          actorSystem = actorSystem
        )

        val reindexService =
          new ReindexService(
            dynamoDBClient = dynamoDbClient,
            dynamoConfig = DynamoConfig(tableName),
            metricsSender = metricsSender,
            versionedDao = new VersionedDao(
              dynamoDbClient = dynamoDbClient,
              DynamoConfig(tableName)),
            indexName = indexName
          )

        testWith(reindexService)
      }
  }


  it("only updates records with a lower than desired reindexVersion") {
    withLocalDynamoDbTableAndIndex { fixtures =>
      val tableName = fixtures.tableName
      val indexName = fixtures.indexName
      withReindexService(tableName, indexName) { reindexService =>
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

        records.foreach(record => Scanamo.put(dynamoDbClient)(tableName)(record))

        val reindexJob = ReindexJob(
          shardId = shardName,
          desiredVersion = desiredVersion
        )

        whenReady(reindexService.runReindex(reindexJob)) { _ =>
          val records = Scanamo.scan[TestRecord](dynamoDbClient)(tableName).map(_.right.get)

          records should contain theSameElementsAs expectedRecords
        }
      }
    }
  }

  it("updates records in the specified shard") {
    withLocalDynamoDbTableAndIndex { fixtures =>
      val tableName = fixtures.tableName
      val indexName = fixtures.indexName
      withReindexService(tableName, indexName) { reindexService =>
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
          Scanamo.put(dynamoDbClient)(tableName)(record))

        val expectedUpdatedRecords = inShardRecords.map(
          record =>
            record
              .copy(
                reindexVersion = desiredVersion,
                version = record.version + 1))

        whenReady(reindexService.runReindex(reindexJob)) { _ =>
          val testRecords =
            Scanamo.scan[TestRecord](dynamoDbClient)(tableName).map(_.right.get)

          testRecords.filter(_.reindexShard != shardName) should contain theSameElementsAs notInShardRecords
          testRecords.filter(_.reindexShard == shardName) should contain theSameElementsAs expectedUpdatedRecords
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    withReindexService("does-not-exist", "no-such-index") { service =>
      val reindexJob = ReindexJob(
        shardId = "sierra/000",
        desiredVersion = 2
      )

      val future = service.runReindex(reindexJob)
      whenReady(future.failed) {
        _ shouldBe a[ResourceNotFoundException]
      }
    }
  }
}
