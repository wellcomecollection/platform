package uk.ac.wellcome.platform.matcher.messages

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.models.VersionExpectedConflictException
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

import scala.concurrent.{ExecutionContext, Future}

class MatcherMessageReceiver @Inject()(
  messageStream: SQSStream[NotificationMessage],
  snsWriter: SNSWriter,
  s3TypeStore: ObjectStore[TransformedBaseWork],
  storageS3Config: S3Config,
  actorSystem: ActorSystem,
  workMatcher: WorkMatcher)(implicit ec: ExecutionContext)
    extends Logging {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(notificationMessage: NotificationMessage): Future[Unit] = {
    (for {
      hybridRecord <- Future.fromTry(
        fromJson[HybridRecord](notificationMessage.Message))
      work <- s3TypeStore.get(
        ObjectLocation(storageS3Config.bucketName, hybridRecord.s3key))
      identifiersList <- workMatcher.matchWork(work)
      _ <- snsWriter.writeMessage(
        message = identifiersList,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()).recover {
      case e: VersionExpectedConflictException =>
        debug(s"Not matching work due to version: ${e.getMessage}")
    }
  }

  def stop(): Future[Terminated] = {
    actorSystem.terminate()
  }
}
