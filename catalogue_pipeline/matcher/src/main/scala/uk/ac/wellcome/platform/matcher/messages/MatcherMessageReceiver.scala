package uk.ac.wellcome.platform.matcher.messages

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.models.VersionConflictException
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

class MatcherMessageReceiver @Inject()(
  messageStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  s3TypeStore: ObjectStore[RecorderWorkEntry],
  storageS3Config: S3Config,
  actorSystem: ActorSystem,
  workMatcher: WorkMatcher)
    extends Logging {

  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(notificationMessage: NotificationMessage): Future[Unit] = {
    (for {
      hybridRecord <- Future.fromTry(
        fromJson[HybridRecord](notificationMessage.Message))
      workEntry <- s3TypeStore.get(
        ObjectLocation(storageS3Config.bucketName, hybridRecord.s3key))
      identifiersList <- workMatcher.matchWork(workEntry.work)
      _ <- snsWriter.writeMessage(
        message = toJson(identifiersList).get,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()).recover {
      case e: VersionConflictException => info(e.getMessage)
    }
  }

  def stop(): Future[Terminated] = {
    actorSystem.terminate()
  }
}
