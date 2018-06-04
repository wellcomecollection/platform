package uk.ac.wellcome.platform.snapshot_generator.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotJob
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SnapshotGeneratorWorkerService @Inject()(
  snapshotService: SnapshotService,
  sqsStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  system: ActorSystem
) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      snapshotJob <- Future.fromTry(fromJson[SnapshotJob](message.Message))
      completedSnapshotJob <- snapshotService.generateSnapshot(
        snapshotJob = snapshotJob)
      message <- Future.fromTry(toJson(completedSnapshotJob))
      _ <- snsWriter.writeMessage(
        subject = s"source: ${this.getClass.getSimpleName}.processMessage",
        message = message
      )
    } yield ()

  def stop() = system.terminate()
}
