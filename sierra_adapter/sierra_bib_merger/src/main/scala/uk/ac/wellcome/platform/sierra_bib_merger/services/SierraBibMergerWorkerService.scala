package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.messaging.sqs.{SQSReader, SQSWorkerToDynamo}
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import io.circe.generic.extras.semiauto._
import uk.ac.wellcome.monitoring.MetricsSender

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
