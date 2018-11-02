package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      bibRecord <- Future.fromTry(fromJson[SierraBibRecord](message.body))
      hybridRecord <- sierraBibMergerUpdaterService.update(bibRecord)
      _ <- snsWriter.writeMessage(
        hybridRecord,
        s"Sent from ${this.getClass.getSimpleName}")
    } yield ()

  def stop() = system.terminate()
}
