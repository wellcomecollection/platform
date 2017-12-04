package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SierraBibRecord
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorker(reader, system, metrics) with Logging {

  override def processMessage(message: SQSMessage): Future[Unit] =
    JsonUtil.fromJson[SierraBibRecord](message.body) match {
      case Success(record) => sierraBibMergerUpdaterService.update(record)
      case Failure(e) => Future(logger.warn(s"Failed processing $message", e))
    }
}
