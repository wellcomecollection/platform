package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.platform.reindex.reindex_worker.services.{BulkSNSSender, RecordReader, ReindexWorkerService}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends Akka with DynamoFixtures with SNS with SQS {
  def withWorkerService[R](queue: Queue, table: Table)(
    testWith: TestWith[ReindexWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withSQSStream[NotificationMessage, R](actorSystem, queue) { sqsStream =>
        withMaxRecordsScanner(table) { maxRecordsScanner =>
          withParallelScanner(table) { parallelScanner =>
            val recordReader = new RecordReader(
              maxRecordsScanner = maxRecordsScanner,
              parallelScanner = parallelScanner
            )

            withSNSMessageWriter { snsMessageWriter =>
              val bulkSNSSender = new BulkSNSSender(
                snsMessageWriter = snsMessageWriter
              )

              val workerService = new ReindexWorkerService(
                recordReader = recordReader,
                bulkSNSSender = bulkSNSSender,
                sqsStream = sqsStream
              )(actorSystem = actorSystem, ec = global)

              workerService.run()

              testWith(workerService)
            }
          }
        }
      }
    }
}
