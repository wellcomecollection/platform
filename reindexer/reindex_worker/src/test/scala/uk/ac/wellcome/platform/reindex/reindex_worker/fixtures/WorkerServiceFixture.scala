package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.platform.reindex.reindex_worker.services.ReindexWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with BulkSNSSenderFixture
    with RecordReaderFixture
    with SQS {
  def withWorkerService[R](queue: Queue, table: Table, topic: Topic)(
    testWith: TestWith[ReindexWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withSQSStream[NotificationMessage, R](actorSystem, queue) { sqsStream =>
        withRecordReader { recordReader =>
          withBulkSNSSender { bulkSNSSender =>
            val workerService = new ReindexWorkerService(
              recordReader = recordReader,
              bulkSNSSender = bulkSNSSender,
              sqsStream = sqsStream,
              dynamoConfig = createDynamoConfigWith(table),
              snsConfig = createSNSConfigWith(topic)
            )(actorSystem = actorSystem, ec = global)

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }
}
