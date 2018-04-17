package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import com.amazonaws.services.s3.AmazonS3

class SnapshotConvertorWorkerService @Inject()(
  convertorService: ConvertorService,
  reader: SQSReader,
  snsWriter: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender,
  s3: AmazonS3
) extends SQSWorker(reader, system, metrics, s3) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      conversionJob <- Future.fromTry(fromJson[ConversionJob](message.body))
      completedConversionJob <- convertorService.runConversion(
        conversionJob = conversionJob)
      message <- Future.fromTry(toJson(completedConversionJob))
      _ <- snsWriter.writeMessage(
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
        message = message
      )
    } yield ()
}
