package uk.ac.wellcome.platform.sierra_item_merger.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.services.{
  SierraItemMergerUpdaterService,
  SierraItemMergerWorkerService
}
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait SierraItemMergerFixtures
    extends Akka
    with LocalVersionedHybridStore
    with SNS
    with SQS
    with SierraAdapterHelpers {
  def withSierraUpdaterService[R](
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      EmptyMetadata,
                                      ObjectStore[SierraTransformable]])(
    testWith: TestWith[SierraItemMergerUpdaterService, R]): R = {
    val sierraUpdaterService = new SierraItemMergerUpdaterService(
      versionedHybridStore = hybridStore
    )
    testWith(sierraUpdaterService)
  }

  def withSierraWorkerService[R](
    queue: Queue,
    topic: Topic,
    sierraDataBucket: Bucket,
    table: Table)(testWith: TestWith[SierraItemMergerWorkerService, R]): R =
    withSierraVHS(sierraDataBucket, table) { vhs =>
      withSierraUpdaterService(vhs) { updaterService =>
        withActorSystem { implicit actorSystem =>
          withSQSStream[NotificationMessage, R](queue) { sqsStream =>
            withSNSWriter(topic) { snsWriter =>
              val workerService = new SierraItemMergerWorkerService(
                sqsStream = sqsStream,
                sierraItemMergerUpdaterService = updaterService,
                objectStore = ObjectStore[SierraItemRecord],
                snsWriter = snsWriter
              )

              testWith(workerService)
            }
          }
        }
      }
    }
}
