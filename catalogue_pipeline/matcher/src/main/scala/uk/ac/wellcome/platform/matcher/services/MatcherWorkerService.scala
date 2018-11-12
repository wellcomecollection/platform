package uk.ac.wellcome.platform.matcher.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.models.VersionExpectedConflictException

import scala.concurrent.{ExecutionContext, Future}

class MatcherWorkerService @Inject()(
  messageStream: MessageStream[TransformedBaseWork],
  snsWriter: SNSWriter,
  actorSystem: ActorSystem,
  workMatcher: WorkMatcher)(implicit ec: ExecutionContext)
    extends Logging {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(work: TransformedBaseWork): Future[Unit] = {
    (for {
      identifiersList <- workMatcher.matchWork(work)
      _ <- snsWriter.writeMessage(
        message = identifiersList,
        subject = s"source: ${this.getClass.getSimpleName}.processMessage"
      )
    } yield ()).recover {
      case e: VersionExpectedConflictException =>
        debug(
          s"Not matching work due to version conflict exception: ${e.getMessage}")
    }
  }

  def stop(): Future[Terminated] = {
    actorSystem.terminate()
  }
}
