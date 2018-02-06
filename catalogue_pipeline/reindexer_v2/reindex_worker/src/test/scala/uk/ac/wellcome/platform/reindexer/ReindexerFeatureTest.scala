package uk.ac.wellcome.platform.reindexer

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.VersionUpdater
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.platform.reindexer.models.ReindexJob
import uk.ac.wellcome.storage.VersionedHybridStoreLocal
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil._

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with VersionedHybridStoreLocal
    with AmazonCloudWatchFlag
    with SQSLocal
    with ScalaFutures {

  val bucketName = "reindexer-feature-test-bucket"
  val tableName = "reindexer-feature-test-table"
  val queueUrl = createQueueAndReturnUrl("reindexer-feature-test-q")

  implicit val sierraTransformableUpdater =
    new VersionUpdater[MiroTransformable] {
      override def updateVersion(miroTransformable: MiroTransformable,
                                 newVersion: Int): MiroTransformable = {
        miroTransformable.copy(version = newVersion)
      }
    }

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.tableName" -> tableName
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
    )

  val currentVersion = 1
  val desiredVersion = 5

  it("should increase the reindexVersion on every record that needs a reindex") {
    val numberOfRecords = 4

    (1 to numberOfRecords).map { i =>
      hybridStore.updateRecord[MiroTransformable](
        MiroTransformable(
          sourceId = s"V00000$i",
          MiroCollection = "images-V",
          data = """{"title": "A visualisation of various vultures"}""",
          reindexShard = "miro",
          reindexVersion = 1
        )
      )
    }

    val reindexJob = ReindexJob(
      shardId = "miro",
      desiredVersion = desiredVersion
    )

    server.start()
    sqsClient.sendMessage(queueUrl, toJson(reindexJob).get)

    eventually {
      (1 to numberOfRecords).map { i =>
        val recordFuture = hybridStore.getRecord[MiroTransformable](id = s"miro/V00000$i")
        whenReady(recordFuture) { record: Option[MiroTransformable] =>
          record.get.reindexVersion shouldBe desiredVersion
        }
      }
    }

    server.close()
  }
}
