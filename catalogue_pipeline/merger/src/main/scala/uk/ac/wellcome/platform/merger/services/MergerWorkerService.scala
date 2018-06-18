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
      maybeRecorderWorkEntries <- getFromVHS(matcherResult)
      works = worksToSend(matcherResult, maybeRecorderWorkEntries)
      _ <- sendWorks(works)
    } yield ()

  private def getWorksIdentifiers(matcherResult: MatcherResult) = {
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier
  }

  private def getFromVHS(matcherResult: MatcherResult): Future[Set[(WorkIdentifier, Option[RecorderWorkEntry])]] = {
    val worksIdentifiers = getWorksIdentifiers(matcherResult)
    Future.sequence(worksIdentifiers.map(workIdentifier => vhs.getRecord(workIdentifier.identifier).map((workIdentifier, _))))
  }

  private def worksToSend(matcherResult: MatcherResult, maybeRecorderWorkEntries: Set[(WorkIdentifier, Option[RecorderWorkEntry])]): Option[Set[UnidentifiedWork]] = {
    val worksIdentifiers = getWorksIdentifiers(matcherResult)
    val receivedIdVersionSet = worksIdentifiers.map{ workIdentifier => (workIdentifier.identifier, workIdentifier.version)}

    val receivedWorksWithZeroId = worksIdentifiers.filter { _.version == 0 }

    receivedWorksWithZeroId.toSeq match {
      case Nil => {
        val vhsIdToWorkSet = maybeRecorderWorkEntries.map { case (identifier, maybeRecorderWorkEntry) =>
          (identifier.identifier, maybeRecorderWorkEntry.getOrElse(throw new RuntimeException(s"Work $identifier is not in vhs!")).work)
        }
        val vhsIdToVersionSet = vhsIdToWorkSet.map{case (id, work) => (id, work.version)}
        (receivedIdVersionSet diff vhsIdToVersionSet).toSeq match {
          case Nil => Some(vhsIdToWorkSet.map{case (_, work) => work})
          case _ =>
            debug(" Different versions in vhs: discarding message")
            None
        }
      }
      case _ => None
    }
  }

  private def sendWorks(maybeWorks: Option[Set[UnidentifiedWork]]) = {
    maybeWorks match {
      case Some(works) => Future.sequence(works.map(work => SNSWriter.writeMessage(toJson(work).get, "merged-work"))).map (_ => ())
      case None => Future.successful(())
    }

  }


  def stop() = system.terminate()
}
