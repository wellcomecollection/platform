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
      identifiers = getWorksIdentifiers(matcherResult)
      maybeRecorderWorkEntries <- Future.sequence(identifiers.map(identifier => vhs.getRecord(identifier)))
      works = maybeRecorderWorkEntries.map(_.get.work)
      _ <- Future.sequence(works.map( work => SNSWriter.writeMessage(toJson(work).get, "merged-work")))
    } yield ()

  private def getWorksIdentifiers(matcherResult: MatcherResult) = {
//    matcherResult.works.flatMap(matchedIdentifiers => matchedIdentifiers.identifiers.map(_.identifier))
    for {
      matchedIdentifiers <- matcherResult.works
      workIdentifier <- matchedIdentifiers.identifiers
    } yield workIdentifier.identifier
  }

  def stop() = system.terminate()
}
