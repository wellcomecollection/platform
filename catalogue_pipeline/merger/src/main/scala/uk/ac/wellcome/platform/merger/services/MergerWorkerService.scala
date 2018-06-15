package uk.ac.wellcome.platform.merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.matcher.MatcherResult
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.{ExecutionContext, Future}

class MergerWorkerService @Inject()(
                                     system: ActorSystem,
                                     sqsStream: SQSStream[NotificationMessage],
                                     vhs: VersionedHybridStore[RecorderWorkEntry, EmptyMetadata, ObjectStore[RecorderWorkEntry]],
                                     SNSWriter: SNSWriter
)(implicit context: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      matcherResult <-Future.fromTry(fromJson[MatcherResult] (message.Message))
      workIdentifiers = getWorksIdentifiers(matcherResult)
      maybeRecorderWorkEntries <- Future.sequence(workIdentifiers.map(workIdentifier => vhs.getRecord(workIdentifier.identifier).map((workIdentifier, _))))
      works = maybeRecorderWorkEntries.map{ case (identifier, maybeRecorderWorkEntry) => maybeRecorderWorkEntry.getOrElse(throw new RuntimeException(s"Work $identifier is not in vhs!")).work}
      _ <- Future.sequence(works.map( work => SNSWriter.writeMessage(toJson(work).get, "merged-work")))
    } yield ()

  private def getWorksIdentifiers(matcherResult: MatcherResult) = {
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier
  }

  def stop() = system.terminate()
}
