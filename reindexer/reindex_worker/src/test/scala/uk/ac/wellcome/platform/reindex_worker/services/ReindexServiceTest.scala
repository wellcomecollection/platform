package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with DynamoDBLocal[HybridRecord]
    with MockitoSugar
    with ExtendedPatience {

  override lazy val tableName: String = "table"

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests",
                      mock[AmazonCloudWatch],
                      ActorSystem())

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Sourced
    .toSourcedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  it("only updates records with a lower than desired reindexVersion") {
    val currentVersion = 1
    val desiredVersion = 2

    val shardName = "shard"

    val exampleRecord = HybridRecord(version = 1,
                                     sourceId = "id",
                                     sourceName = "source",
                                     s3key = "s3://bucket/key",
                                     reindexShard = shardName,
                                     reindexVersion = currentVersion)

    val newerRecord = exampleRecord.copy(
      sourceId = "id1",
      reindexVersion = desiredVersion + 1
    )

    val olderRecord = exampleRecord.copy(
      sourceId = "id2"
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
      Scanamo.put(dynamoDbClient)(tableName)(record)(enrichedDynamoFormat))

    val reindexService =
      new ReindexService(
        dynamoDBClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(tableName),
        metricsSender = metricsSender,
        versionedDao = new VersionedDao(dynamoDbClient = dynamoDbClient,
                                        DynamoConfig(tableName))
      )

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    whenReady(reindexService.runReindex(reindexJob)) { _ =>
      val hybridRecords =
        Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

      hybridRecords should contain theSameElementsAs expectedRecords
    }
  }

  it("updates records in the specified shard") {
    val currentVersion = 1
    val desiredVersion = 2

    val shardName = "shard"

    val exampleRecord = HybridRecord(version = 1,
                                     sourceId = "id",
                                     sourceName = "source",
                                     s3key = "s3://bucket/key",
                                     reindexShard = shardName,
                                     reindexVersion = currentVersion)

    val inShardRecords = List(
      exampleRecord.copy(sourceId = "id1"),
      exampleRecord.copy(sourceId = "id2")
    )

    val notInShardRecords = List(
      exampleRecord.copy(sourceId = "id3",
                         reindexShard = "not_the_same_shard"),
      exampleRecord.copy(sourceId = "id4", reindexShard = "not_the_same_shard")
    )

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    val recordList = inShardRecords ++ notInShardRecords

    recordList.foreach(record =>
      Scanamo.put(dynamoDbClient)(tableName)(record)(enrichedDynamoFormat))

    val expectedUpdatedRecords = inShardRecords.map(
      record =>
        record.copy(reindexVersion = desiredVersion,
                    version = record.version + 1))

    val reindexService =
      new ReindexService(
        dynamoDBClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(tableName),
        metricsSender = metricsSender,
        versionedDao = new VersionedDao(dynamoDbClient = dynamoDbClient,
                                        DynamoConfig(tableName))
      )

    whenReady(reindexService.runReindex(reindexJob)) { _ =>
      val hybridRecords =
        Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

      hybridRecords.filter(_.reindexShard != shardName) should contain theSameElementsAs notInShardRecords
      hybridRecords.filter(_.reindexShard == shardName) should contain theSameElementsAs expectedUpdatedRecords
    }
  }
}
