package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker, SQSWorkerToDynamo}
import uk.ac.wellcome.circe._
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraRecord}

import scala.concurrent.Future

class SierraBibMergerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  sierraBibMergerUpdaterService: SierraBibMergerUpdaterService
) extends SQSWorkerToDynamo[SierraRecord](reader, system, metrics) {

  override def conversion(s: String): Either[circe.Error, SierraRecord] =
    decode[SierraRecord](s)

  override def process(record: SierraRecord): Future[Unit] =
    sierraBibMergerUpdaterService.update(record.toBibRecord)

}
