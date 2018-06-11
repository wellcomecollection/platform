package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SierraItemsToDynamoWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[SierraRecord],
  dynamoInserter: DynamoInserter
) {

  sqsStream.foreach(this.getClass.getSimpleName, store)

  private def store(record: SierraRecord): Future[Unit] =
    dynamoInserter.insertIntoDynamo(record.toItemRecord.get)

  def stop() = system.terminate()
}
