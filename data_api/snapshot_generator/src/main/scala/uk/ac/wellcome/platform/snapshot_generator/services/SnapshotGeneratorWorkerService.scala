package uk.ac.wellcome.platform.snapshot_generator.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotJob
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SnapshotGeneratorWorkerService @Inject()(
  snapshotService: SnapshotService,
  sqsStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  system: ActorSystem
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      snapshotJob <- Future.fromTry(fromJson[SnapshotJob](message.body))
      completedSnapshotJob <- snapshotService.generateSnapshot(
        snapshotJob = snapshotJob)
      _ <- snsWriter.writeMessage(
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
        message = completedSnapshotJob
      )
    } yield ()

  def stop() = system.terminate()
}
