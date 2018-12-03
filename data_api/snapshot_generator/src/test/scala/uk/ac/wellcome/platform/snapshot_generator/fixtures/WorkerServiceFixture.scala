package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.Suite
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.platform.snapshot_generator.services.SnapshotGeneratorWorkerService
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends SnapshotServiceFixture with SNS with SQS { this: Suite =>
  def withWorkerService[R](queue: Queue, topic: Topic, indexNameV1: String, indexNameV2: String)(testWith: TestWith[SnapshotGeneratorWorkerService, R])(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): R =
    withSnapshotService(indexNameV1, indexNameV2) { snapshotService =>
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
