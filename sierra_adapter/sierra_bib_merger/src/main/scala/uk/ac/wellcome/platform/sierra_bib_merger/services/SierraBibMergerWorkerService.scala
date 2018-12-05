package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.Done
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerWorkerService(
  sqsStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
)(implicit ec: ExecutionContext)
    extends Runnable {
  private def process(message: NotificationMessage): Future[Unit] =
    for {
      bibRecord <- Future.fromTry(fromJson[SierraBibRecord](message.body))
      vhsIndexEntry <- sierraBibMergerUpdaterService.update(bibRecord)
      _ <- snsWriter.writeMessage(
        vhsIndexEntry.hybridRecord,
        s"Sent from ${this.getClass.getSimpleName}")
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, process)
}
