package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.generic.extras.auto._
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker}
import uk.ac.wellcome.sierra_adapter.circe._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorker(reader, system, metrics)
    with Logging {

  override def processMessage(message: SQSMessage): Future[Unit] =
    // Using Circe here because Jackson creates nulls for empty lists
    decode[SierraItemRecord](message.body) match {
      case Right(record) => sierraItemMergerUpdaterService.update(record)
      case Left(e) =>
        Future {
          logger.warn(s"Failed processing $message", e)
          throw SQSReaderGracefulException(e)
        }
    }
}
