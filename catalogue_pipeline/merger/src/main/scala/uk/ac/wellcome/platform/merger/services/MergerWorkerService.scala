package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.{MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._
import cats.implicits._

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
                                     system: ActorSystem,
                                     sqsStream: SQSStream[NotificationMessage],
                                     vhs: VersionedHybridStore[RecorderWorkEntry, EmptyMetadata, ObjectStore[RecorderWorkEntry]],
                                     SNSWriter: SNSWriter
)(implicit context: ExecutionContext) extends Logging{

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <-Future.fromTry(fromJson[MatcherResult] (message.Message))
      maybeWorkEntries <- getFromVHS(matcherResult)
      maybeWorks = maybeWorkEntries.map { workEntries =>
        workEntries.map { _.work }
      }
      _ <- sendWorks(maybeWorks)
    } yield ()


  private def getFromVHS(matcherResult: MatcherResult): Future[Option[List[RecorderWorkEntry]]] = {
    val worksIdentifiers = getWorksIdentifiers(matcherResult)

    // If we get an identifier with version 0 from the matcher, it means
    // we know the work exists but it hasn't been seen in the pipeline yet.
    //
    // In that case, we have to discard the message because we don't have
    // enough to do a complete merge -- we'll do it when the work appears.
    //
    val missingWorks = worksIdentifiers.filter { _.version == 0 }

    missingWorks.toSeq match {
      case Nil =>
        for {
          maybeWorkEntries <- Future.sequence(worksIdentifiers.map { workId => getRecorderEntryForIdentifier(workId) })
        } yield maybeWorkEntries.toList.sequence

      case _ => Future.successful(None)
    }
  }

  private def getWorksIdentifiers(matcherResult: MatcherResult): Set[WorkIdentifier] = {
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier
  }

  // This function wraps fetching a record from VHS.  It:
  //
  //    - returns Some[RecorderWorkEntry] if the entry is in VHS and the correct version
  //    - returns None if the entry is in VHS and the wrong version
  //
  private def getRecorderEntryForIdentifier(workIdentifier: WorkIdentifier): Future[Option[RecorderWorkEntry]] = {
    vhs.getRecord(id = workIdentifier.identifier).map {
      case None => throw new RuntimeException(s"Work ${workIdentifier.identifier} is not in vhs!")
      case Some(record) =>
        record.work.version match {
          case workIdentifier.version => Some(record)
          case _ =>
            debug(s"VHS version = ${record.work.version}, identifier version = ${workIdentifier.version}, so discarding message")
            None
        }
    }
  }

  private def sendWorks(maybeWorks: Option[Seq[UnidentifiedWork]]) = {
    maybeWorks match {
      case Some(works) =>
        Future.sequence(works.map(work => SNSWriter.writeMessage(toJson(work).get, "merged-work"))).map (_ => ())
      case None => Future.successful(())
    }

  }

  def stop() = system.terminate()
}
