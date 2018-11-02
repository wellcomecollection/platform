package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraItemsToDynamoWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  dynamoInserter: DynamoInserter,
  snsWriter: SNSWriter
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      itemRecord <- Future.fromTry(fromJson[SierraItemRecord](message.body))
      hybridRecord <- dynamoInserter.insertIntoDynamo(itemRecord)
      _ <- snsWriter.writeMessage(
        message = hybridRecord,
        subject = s"Sent from ${this.getClass.getSimpleName}"
      )
    } yield ()

  def stop() = system.terminate()
}
