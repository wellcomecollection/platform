package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      record <- Future.fromTry(fromJson[SierraBibRecord](message.Message))
      _ <- sierraBibMergerUpdaterService.update(record)
    } yield ()

  def stop() = system.terminate()
}
