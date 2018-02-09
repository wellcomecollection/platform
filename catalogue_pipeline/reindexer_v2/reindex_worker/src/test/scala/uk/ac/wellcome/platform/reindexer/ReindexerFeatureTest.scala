package uk.ac.wellcome.platform.reindexer

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.Versioned
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindexer.models.ReindexJob
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil._

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually with DynamoDBLocal[HybridRecord]
    with AmazonCloudWatchFlag
    with SQSLocal
    with ScalaFutures {

  override lazy val tableName: String = "table"

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Versioned
    .toVersionedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  val queueUrl = createQueueAndReturnUrl("reindexer-feature-test-q")

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.tableName" -> tableName,
        "aws.sqs.queue.url" -> queueUrl
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags ++ sqsLocalFlags
    )

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  it("increases the reindexVersion on every record that needs a reindex") {
    val numberOfRecords = 4

    val putRecords = (1 to numberOfRecords).map(i => {
      val record = HybridRecord(version = 1,
        sourceId = s"id$i",
        sourceName = "source",
        s3key = "s3://bucket/key",
        reindexShard = shardName,
        reindexVersion = currentVersion)

      Scanamo.put(dynamoDbClient)(tableName)(record)(enrichedDynamoFormat)

      record
    })

    val expectedRecords = putRecords.map(record => record.copy(reindexVersion = desiredVersion, version = record.version + 1))

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    server.start()
    val sqsMessage = SQSMessage(None, toJson(reindexJob).get, "topic", "message", "now")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    eventually {
      val actualRecords = Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

      actualRecords should contain theSameElementsAs expectedRecords
    }

    server.close()
  }
}
