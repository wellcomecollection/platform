package uk.ac.wellcome.platform.merger.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.{MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{BaseWork, UnidentifiedWork}
import uk.ac.wellcome.json.JsonUtil._

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  playbackService: RecorderPlaybackService,
  merger: Merger,
  messageWriter: MessageWriter[BaseWork]
)(implicit ec: ExecutionContext)
    extends Logging {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <- Future.fromTry(fromJson[MatcherResult](message.Message))
      workIdentifiers = getWorksIdentifiers(matcherResult).toList
      maybeWorkEntries <- playbackService.fetchAllRecorderWorkEntries(workIdentifiers)
      works <- mergeIfAllWorksDefined(maybeWorkEntries)
      _ <- sendWorks(works)
    } yield ()

  private def mergeIfAllWorksDefined(
    maybeWorkEntries: List[Option[RecorderWorkEntry]]) = Future {
    val workEntries = maybeWorkEntries.flatten
    val works = workEntries.map(_.work).collect {
      case unidentifiedWork: UnidentifiedWork => unidentifiedWork
    }
    if (works.size == maybeWorkEntries.size) {
      merger.merge(works)
    } else {
      workEntries.map(_.work)
    }
  }

  private def sendWorks(mergedWorks: Seq[BaseWork]) = {
    Future
      .sequence(
        mergedWorks.map(
          messageWriter.write(_, "merged-work")
        ))
  }

  private def getWorksIdentifiers(
    matcherResult: MatcherResult): Set[WorkIdentifier] = {
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier
  }

  def stop(): Future[Terminated] = system.terminate()
}
