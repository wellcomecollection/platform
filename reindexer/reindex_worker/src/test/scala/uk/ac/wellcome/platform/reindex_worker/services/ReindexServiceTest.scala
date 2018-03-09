package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.test.utils.ExtendedPatience
import scala.concurrent.duration._

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal[TestRecord]
    with MockitoSugar
    with ExtendedPatience {

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

  override lazy val tableName: String = "table"

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem())

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

  it("only updates records with a lower than desired reindexVersion") {
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

    val reindexService =
      new ReindexService(
        dynamoDBClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(tableName),
        metricsSender = metricsSender,
        versionedDao = new VersionedDao(
          dynamoDbClient = dynamoDbClient,
          DynamoConfig(tableName))
      )

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    whenReady(reindexService.runReindex(reindexJob)) { _ =>
      val records =
        Scanamo.scan[TestRecord](dynamoDbClient)(tableName).map(_.right.get)

      records should contain theSameElementsAs expectedRecords
    }
  }

  it("updates records in the specified shard") {
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
          .copy(reindexVersion = desiredVersion, version = record.version + 1))

    val reindexService =
      new ReindexService(
        dynamoDBClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(tableName),
        metricsSender = metricsSender,
        versionedDao = new VersionedDao(
          dynamoDbClient = dynamoDbClient,
          DynamoConfig(tableName))
      )

    whenReady(reindexService.runReindex(reindexJob)) { _ =>
      val testRecords =
        Scanamo.scan[TestRecord](dynamoDbClient)(tableName).map(_.right.get)

      testRecords.filter(_.reindexShard != shardName) should contain theSameElementsAs notInShardRecords
      testRecords.filter(_.reindexShard == shardName) should contain theSameElementsAs expectedUpdatedRecords
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    val service = new ReindexService(
      dynamoDBClient = dynamoDbClient,
      metricsSender = metricsSender,
      versionedDao = new VersionedDao(
        dynamoDbClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(table = "does-not-exist")
      ),
      dynamoConfig = DynamoConfig(table = "does-not-exist")
    )

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
