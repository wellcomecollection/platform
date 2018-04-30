package uk.ac.wellcome.platform.snapshot_generator.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader, SQSWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotJob
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SnapshotGeneratorWorkerService @Inject()(
  snapshotService: SnapshotService,
  reader: SQSReader,
  snsWriter: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      snapshotJob <- Future.fromTry(fromJson[SnapshotJob](message.body))
      completedSnapshotJob <- snapshotService.generateSnapshot(
        snapshotJob = snapshotJob)
      message <- Future.fromTry(toJson(completedSnapshotJob))
      _ <- snsWriter.writeMessage(
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
        message = message
      )
    } yield ()
}
