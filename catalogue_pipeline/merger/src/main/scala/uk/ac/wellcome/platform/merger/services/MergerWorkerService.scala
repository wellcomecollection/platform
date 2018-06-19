package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import cats.implicits._
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.{MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
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
  SNSWriter: SNSWriter
)(implicit context: ExecutionContext)
    extends Logging {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <- Future.fromTry(fromJson[MatcherResult](message.Message))
      eitherMaybeWorkEntries <- getFromVHS(matcherResult)
      eitherWorkEntries = eitherMaybeWorkEntries.map { workEntries =>
        workEntries.collect{case Some(work) => work.work}
      }
      _ <- sendWorks(eitherWorkEntries)
    } yield ()

  private def getFromVHS(matcherResult: MatcherResult): Future[Either[VersionMismatchError, List[Option[RecorderWorkEntry]]]] = {
    val worksIdentifiers = getWorksIdentifiers(matcherResult)
    for {
      maybeWorkEntries <- Future.sequence(worksIdentifiers.map { workId =>
        getRecorderEntryForIdentifier(workId)
      })
    } yield maybeWorkEntries.toList.sequence
  }

  private def sendWorks(eitherWorkEntries: Either[VersionMismatchError,Seq[UnidentifiedWork]]) = {
    eitherWorkEntries match {
      case Right(works) =>
        Future
          .sequence(works.map(work =>
            SNSWriter.writeMessage(toJson(work).get, "merged-work")))
          .map(_ => ())
      case Left(_) => Future.successful(())
    }
  }

  private def getWorksIdentifiers(
    matcherResult: MatcherResult): Set[WorkIdentifier] = {
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier
  }

  private def getRecorderEntryForIdentifier(
    workIdentifier: WorkIdentifier): Future[Either[VersionMismatchError,Option[RecorderWorkEntry]]] = {
    workIdentifier.version match {
      case 0 => Future.successful(Right(None))
      case _ =>
        vhs.getRecord(id = workIdentifier.identifier).map {
          case None =>
            throw new RuntimeException(
              s"Work ${
                workIdentifier.identifier
              } is not in vhs!")
          case Some(record) if record.work.version == workIdentifier.version =>
            Right(Some(record))
          case Some(record) =>
            debug(
              s"VHS version = ${
                record.work.version
              }, identifier version = ${
                workIdentifier.version
              }, so discarding message")
            Left(VersionMismatchError(workIdentifier.identifier, workIdentifier.version, record.work.version))
        }
    }
  }

  def stop() = system.terminate()
}

case class VersionMismatchError(id: String, receivedVersion: Int, vhsVersion:Int)