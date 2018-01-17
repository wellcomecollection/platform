package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
i
import uk.ac.wellcome.sqs.SQSWorkerToDynamo
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.sqs.SQSReader

import scala.concurrent.Future

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorkerToDynamo[SierraRecord](reader, system, metrics) {

  override def store(record: SierraRecord): Future[Unit] =
    sierraBibMergerUpdaterService.update(record.toBibRecord)

}
