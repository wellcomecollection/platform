package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.Done
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraItemsToDynamoWorkerService(
  sqsStream: SQSStream[NotificationMessage],
  dynamoInserter: DynamoInserter,
  snsWriter: SNSWriter
)(implicit ec: ExecutionContext)
    extends Runnable {

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      itemRecord <- Future.fromTry(fromJson[SierraItemRecord](message.body))
      vhsIndexEntry <- dynamoInserter.insertIntoDynamo(itemRecord)
      _ <- snsWriter.writeMessage(
        message = vhsIndexEntry.hybridRecord,
        subject = s"Sent from ${this.getClass.getSimpleName}"
      )
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, process)
}
