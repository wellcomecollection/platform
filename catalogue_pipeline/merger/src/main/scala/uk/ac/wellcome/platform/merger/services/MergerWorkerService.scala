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
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  vhs: VersionedHybridStore[RecorderWorkEntry,
                            EmptyMetadata,
                            ObjectStore[RecorderWorkEntry]],
  merger: Merger,
  messageWriter: MessageWriter[BaseWork]
)(implicit context: ExecutionContext)
    extends Logging {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <- Future.fromTry(fromJson[MatcherResult](message.Message))
      maybeWorkEntries <- getFromVHS(matcherResult)
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

  private def getFromVHS(
    matcherResult: MatcherResult): Future[List[Option[RecorderWorkEntry]]] = {
    val worksIdentifiers = getWorksIdentifiers(matcherResult)
    for {
      maybeWorkEntries <- Future.sequence(worksIdentifiers.toList.map {
        workId =>
          getRecorderEntryForIdentifier(workId)
      })
    } yield maybeWorkEntries
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

  private def getRecorderEntryForIdentifier(
    workIdentifier: WorkIdentifier): Future[Option[RecorderWorkEntry]] = {
    workIdentifier.version match {
      case 0 =>
        Future.successful(None)
      case _ =>
        vhs.getRecord(id = workIdentifier.identifier).map {
          case None =>
            throw new RuntimeException(
              s"Work ${workIdentifier.identifier} is not in vhs!")
          case Some(record) if record.work.version == workIdentifier.version =>
            Some(record)
          case Some(record) =>
            debug(
              s"VHS version = ${record.work.version}, identifier version = ${workIdentifier.version}, so discarding work")
            None
        }
    }
  }

  def stop(): Future[Terminated] = system.terminate()
}
