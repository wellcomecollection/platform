package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.sqs.SQSWorkerToDynamo

import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

import scala.concurrent.Future


class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorkerToDynamo[SierraItemRecord](reader, system, metrics) {

  override def store(record: SierraItemRecord): Future[Unit] =
    sierraItemMergerUpdaterService.update(record)

}

