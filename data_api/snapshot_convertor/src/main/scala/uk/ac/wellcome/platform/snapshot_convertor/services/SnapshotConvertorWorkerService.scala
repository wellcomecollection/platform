package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

case class ConversionJob()

case class CompletedConversionJob()

object CompletedConversionJob {
  def apply(conversionJob: ConversionJob): CompletedConversionJob =
    CompletedConversionJob()
}

class SnapshotConvertorWorkerService @Inject()(
  convertorService: ConvertorService,
  reader: SQSReader,
  snsWriter: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      conversionJob <- Future.fromTry(fromJson[ConversionJob](message.body))
      _ <- convertorService.runConversion(conversionJob = conversionJob)
      message <- Future.fromTry(toJson(CompletedConversionJob(conversionJob)))
      _ <- snsWriter.writeMessage(
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
        message = message
      )
    } yield ()
}
