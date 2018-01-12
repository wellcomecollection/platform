package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker}
import uk.ac.wellcome.circe._
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraRecord}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorker(reader, system, metrics)
    with Logging {

  override def processMessage(message: SQSMessage): Future[Unit] =
    decode[SierraRecord](message.body) match {
      case Right(record) => sierraBibMergerUpdaterService.update(SierraBibRecord(record))
      case Left(e) =>
        Future {
          logger.warn(s"Failed processing $message", e)
          throw SQSReaderGracefulException(e)
        }
    }
}
