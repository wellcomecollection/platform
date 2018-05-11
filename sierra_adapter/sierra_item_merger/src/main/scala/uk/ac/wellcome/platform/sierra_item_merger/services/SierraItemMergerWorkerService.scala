package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.{SQSReader, SQSWorkerToDynamo}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil._
import io.circe.generic.extras.semiauto._
import uk.ac.wellcome.monitoring.MetricsSender

import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorkerToDynamo[SierraItemRecord](reader, system, metrics) {

  override implicit val decoder = deriveDecoder[SierraItemRecord]

  override def store(record: SierraItemRecord): Future[Unit] =
    sierraItemMergerUpdaterService.update(record)
}
