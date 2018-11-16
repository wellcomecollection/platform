package uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures

import akka.actor.ActorSystem
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.{DynamoInserter, SierraItemsToDynamoWorkerService}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

trait WorkerServiceFixture extends SNS with SQS {
  type SierraItemsVHS = VersionedHybridStore[SierraItemRecord,
    EmptyMetadata,
    ObjectStore[SierraItemRecord]]

  def withWorkerService[R](
    versionedHybridStore: SierraItemsVHS,
    topic: Topic,
    queue: Queue,
    actorSystem: ActorSystem,
    metricsSender: MetricsSender
  )(testWith: TestWith[SierraItemsToDynamoWorkerService, R]): R =
    withSQSStream[NotificationMessage, R](actorSystem, queue, metricsSender) {
      sqsStream =>
        val dynamoInserter = new DynamoInserter(versionedHybridStore)
        withSNSWriter(topic) { snsWriter =>
          val service = new SierraItemsToDynamoWorkerService(
            sqsStream = sqsStream,
            dynamoInserter = dynamoInserter,
            snsWriter = snsWriter
          )

          service.run()

          testWith(service)
        }
    }
}
