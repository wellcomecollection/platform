package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SierraBibMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[SierraRecord],
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) {

  sqsStream.foreach(this.getClass.getSimpleName, store)

  private def store(record: SierraRecord): Future[Unit] =
    sierraBibMergerUpdaterService.update(record.toBibRecord)

  def stop() = system.terminate()
}
