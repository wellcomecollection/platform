package uk.ac.wellcome.platform.merger.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult}
import uk.ac.wellcome.models.work.internal.BaseWork
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  playbackService: RecorderPlaybackService,
  mergerManager: MergerManager,
  messageWriter: MessageWriter[BaseWork]
)(implicit ec: ExecutionContext)
    extends Logging {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <- Future.fromTry(fromJson[MatcherResult](message.Message))
      _ <- Future.sequence(matcherResult.works.map { applyMerge })
    } yield ()

  private def applyMerge(matchedIdentifiers: MatchedIdentifiers): Future[Unit] =
    for {
      maybeWorks <- playbackService.fetchAllWorks(
        matchedIdentifiers.identifiers.toList)
      works: Seq[BaseWork] = mergerManager.applyMerge(maybeWorks = maybeWorks)
      _ <- sendWorks(works)
    } yield ()

  private def sendWorks(mergedWorks: Seq[BaseWork]) = {
    Future
      .sequence(
        mergedWorks.map(
          messageWriter.write(_, "merged-work")
        ))
  }

  def stop(): Future[Terminated] = system.terminate()
}
