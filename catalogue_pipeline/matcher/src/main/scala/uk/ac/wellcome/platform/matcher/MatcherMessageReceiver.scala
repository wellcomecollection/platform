package uk.ac.wellcome.platform.matcher

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectLocation, S3TypeStore}
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

case class RedirectList(redirects: List[Redirect])
case class Redirect(target: SourceIdentifier, sources: List[SourceIdentifier])

class MatcherMessageReceiver @Inject()(
  messageStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  s3TypeStore: S3TypeStore[RecorderWorkEntry],
  storageS3Config: S3Config,
  actorSystem: ActorSystem) {

  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(notificationMessage: NotificationMessage): Future[Unit] = {
    for {
      hybridRecord <- Future.fromTry(
        fromJson[HybridRecord](notificationMessage.Message))
      workEntry <- s3TypeStore.get(
        S3ObjectLocation(storageS3Config.bucketName, hybridRecord.s3key))
      _ <- snsWriter.writeMessage(
        message = toJson(RedirectFinder.redirects(workEntry.work)).get,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()
  }

  def stop(): Future[Terminated] = {
    actorSystem.terminate()
  }
}
