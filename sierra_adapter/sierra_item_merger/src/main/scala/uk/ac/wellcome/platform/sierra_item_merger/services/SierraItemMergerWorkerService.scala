package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sqs.{SQSReader, SQSWorker, SQSWorkerToDynamo}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import io.circe.generic.extras.semiauto._

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
