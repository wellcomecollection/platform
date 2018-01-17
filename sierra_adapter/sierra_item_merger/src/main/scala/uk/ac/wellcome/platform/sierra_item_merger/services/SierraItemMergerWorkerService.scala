package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class SierraItemMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) extends SQSWorker(reader, system, metrics)
    with Logging {

  override def processMessage(message: SQSMessage): Future[Unit] =
    Future.fromTry(fromJson[SierraItemRecord](message.body)).map {
      record => sierraItemMergerUpdaterService.update(record)    }
}
