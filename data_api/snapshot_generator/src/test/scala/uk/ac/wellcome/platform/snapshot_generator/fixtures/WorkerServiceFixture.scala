package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.platform.snapshot_generator.services.SnapshotGeneratorWorkerService
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends AkkaS3
    with SnapshotServiceFixture
    with SNS
    with SQS { this: Suite =>
  def withWorkerService[R](
    queue: Queue,
    topic: Topic,
    indexV1: Index,
    indexV2: Index)(testWith: TestWith[SnapshotGeneratorWorkerService, R])(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): R =
    withS3AkkaClient { s3AkkaClient =>
      withSnapshotService(s3AkkaClient, indexV1, indexV2) { snapshotService =>
        withSQSStream[NotificationMessage, R](queue) { sqsStream =>
          withSNSWriter(topic) { snsWriter =>
            val workerService = new SnapshotGeneratorWorkerService(
              snapshotService = snapshotService,
              sqsStream = sqsStream,
              snsWriter = snsWriter
            )

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }
}
