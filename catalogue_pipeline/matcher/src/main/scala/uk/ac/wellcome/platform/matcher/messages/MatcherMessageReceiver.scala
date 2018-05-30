package uk.ac.wellcome.platform.matcher.messages

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.platform.matcher.matcher.LinkedWorkMatcher
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectLocation, S3TypeStore}
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

class MatcherMessageReceiver @Inject()(
  messageStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  s3TypeStore: S3TypeStore[RecorderWorkEntry],
  storageS3Config: S3Config,
  actorSystem: ActorSystem,
  linkedWorkMatcher: LinkedWorkMatcher) {

  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(notificationMessage: NotificationMessage): Future[Unit] = {
    for {
      hybridRecord <- Future.fromTry(
        fromJson[HybridRecord](notificationMessage.Message))
      s3ObjectLocation = S3ObjectLocation(
        bucket = storageS3Config.bucketName,
        key = hybridRecord.s3key
      )
      workEntry <- s3TypeStore.get(s3ObjectLocation)

      work = workEntry.work
      identifiersList <- linkedWorkMatcher.matchWork(work)

      _ <- snsWriter.writeMessage(
        message = toJson(identifiersList).get,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()
  }

  def stop(): Future[Terminated] = {
    actorSystem.terminate()
  }
}
