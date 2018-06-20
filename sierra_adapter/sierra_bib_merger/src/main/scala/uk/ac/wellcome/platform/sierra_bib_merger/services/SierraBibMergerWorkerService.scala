package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.storage.GlobalExecutionContext
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraBibMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) {

  implicit val ec: ExecutionContext = GlobalExecutionContext.context

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      record <- Future.fromTry(fromJson[SierraRecord](message.Message))
      _ <- sierraBibMergerUpdaterService.update(record.toBibRecord)
    } yield ()

  def stop() = system.terminate()
}
