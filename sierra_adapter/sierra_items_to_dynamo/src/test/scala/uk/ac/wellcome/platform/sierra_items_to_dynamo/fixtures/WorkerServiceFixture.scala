package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.SierraItemsToDynamoWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with SNS
    with SQS
    with DynamoInserterFixture {
  def withWorkerService[R](
    queue: Queue,
    table: Table,
    bucket: Bucket,
    topic: Topic)(testWith: TestWith[SierraItemsToDynamoWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withWorkerService(queue, table, bucket, topic, metricsSender) {
          workerService =>
            testWith(workerService)
        }
      }
    }

  def withWorkerService[R](queue: Queue,
                           table: Table,
                           bucket: Bucket,
                           topic: Topic,
                           metricsSender: MetricsSender)(
    testWith: TestWith[SierraItemsToDynamoWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue, metricsSender) { sqsStream =>
        withDynamoInserter(table, bucket) { dynamoInserter =>
          withSNSWriter(topic) { snsWriter =>
            val workerService = new SierraItemsToDynamoWorkerService(
              sqsStream = sqsStream,
              dynamoInserter = dynamoInserter,
              snsWriter = snsWriter
            )

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }
}
