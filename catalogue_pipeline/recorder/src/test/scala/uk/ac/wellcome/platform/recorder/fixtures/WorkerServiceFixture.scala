package uk.ac.wellcome.platform.recorder.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with LocalVersionedHybridStore
    with Messaging
    with MetricsSenderFixture
    with SNS {
  def withWorkerService[R](
    table: Table,
    storageBucket: Bucket,
    topic: Topic,
    queue: Queue)(testWith: TestWith[RecorderWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withSNSWriter(topic) { snsWriter =>
          withTypeVHS[TransformedBaseWork, EmptyMetadata, R](
            bucket = storageBucket,
            table = table) { versionedHybridStore =>
            withMessageStream[TransformedBaseWork, R](
              queue = queue,
              metricsSender = metricsSender) { messageStream =>
              val workerService = new RecorderWorkerService(
                versionedHybridStore = versionedHybridStore,
                messageStream = messageStream,
                snsWriter = snsWriter
              )

              workerService.run()

              testWith(workerService)
            }
          }
        }
      }
    }
}
