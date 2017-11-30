package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_bib_merger.models.MergedSierraObject
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    JsonUtil.fromJson[MergedSierraObject](message.body) match {
      case Success(mergedSierraObject) => {
        sierraBibMergerUpdaterService.update(mergedSierraObject)
        Future.successful(Unit)
      }
      case Failure(e) => {
        Future.successful(e)
      }
    }
}
