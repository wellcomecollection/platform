package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.generic.extras.auto._
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker, WriteSQSToDynamo}
import uk.ac.wellcome.circe._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorker(reader, system, metrics)
  with WriteSQSToDynamo
    with Logging {

  private def processSierraItemRecord(record: SierraItemRecord) =
    sierraItemMergerUpdaterService.update(record)

  override def processMessage(message: SQSMessage): Future[Unit] =
    convertAndProcess(message, decode[SierraItemRecord], processSierraItemRecord)
}
