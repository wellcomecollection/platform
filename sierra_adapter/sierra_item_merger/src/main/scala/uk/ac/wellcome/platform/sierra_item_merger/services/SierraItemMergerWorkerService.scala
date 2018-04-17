package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.sqs.SQSWorkerToDynamo
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import com.amazonaws.services.s3.AmazonS3

import io.circe.generic.extras.semiauto._

import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService,
  s3: AmazonS3
) extends SQSWorkerToDynamo[SierraItemRecord](reader, system, metrics, s3) {

  override implicit val decoder = deriveDecoder[SierraItemRecord]

  override def store(record: SierraItemRecord): Future[Unit] =
    sierraItemMergerUpdaterService.update(record)
}
