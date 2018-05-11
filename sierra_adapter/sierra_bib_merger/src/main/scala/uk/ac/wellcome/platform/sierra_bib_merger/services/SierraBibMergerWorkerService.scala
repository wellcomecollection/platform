package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.{SQSReader, SQSWorkerToDynamo}
import uk.ac.wellcome.utils.JsonUtil._
import io.circe.generic.extras.semiauto._
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

import scala.concurrent.Future

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorkerToDynamo[SierraRecord](reader, system, metrics) {

  override implicit val decoder = deriveDecoder[SierraRecord]

  override def store(record: SierraRecord): Future[Unit] =
    sierraBibMergerUpdaterService.update(record.toBibRecord)

}
