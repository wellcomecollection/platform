package uk.ac.wellcome.platform.sierra_bib_merger.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.platform.sierra_bib_merger.services.{
  SierraBibMergerUpdaterService,
  SierraBibMergerWorkerService
}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with LocalVersionedHybridStore
    with SNS
    with SQS {
  def withApp[R](bucket: Bucket, table: Table, queue: Queue, topic: Topic)(
    testWith: TestWith[SierraBibMergerWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withTypeVHS[SierraTransformable, EmptyMetadata, R](bucket, table) {
        versionedHybridStore =>
          val updaterService = new SierraBibMergerUpdaterService(
            versionedHybridStore = versionedHybridStore
          )

          withSQSStream[NotificationMessage, R](queue) {
            sqsStream =>
              withSNSWriter(topic) { snsWriter =>
                val workerService = new SierraBibMergerWorkerService(
                  sqsStream = sqsStream,
                  snsWriter = snsWriter,
                  sierraBibMergerUpdaterService = updaterService
                )

                testWith(workerService)
              }
          }
      }
    }
}
