package uk.ac.wellcome.platform.merger

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier
}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MergerTestUtils { this: SQS with SNS with Messaging =>

  def matcherResultWith(matchedEntries: Set[Set[RecorderWorkEntry]]) =
    MatcherResult(
      matchedEntries.map(
        recorderWorkEntries =>
          MatchedIdentifiers(
            recorderWorkEntries.map(workEntry =>
              WorkIdentifier(
                identifier = workEntry.id,
                version = workEntry.work.version)))))

  def sendSQSMessage(queue: SQS.Queue, matcherResult: MatcherResult) = {
    val notificationMessage = NotificationMessage(
      MessageId = "MessageId",
      TopicArn = "topic-arn",
      Subject = "subject",
      Message = toJson(matcherResult).get
    )
    sqsClient.sendMessage(queue.url, toJson(notificationMessage).get)
  }

  def storeInVHS(vhs: VersionedHybridStore[RecorderWorkEntry,
                                           EmptyMetadata,
                                           ObjectStore[RecorderWorkEntry]],
                 entries: List[RecorderWorkEntry]) = {
    Future.sequence(entries.map { recorderWorkEntry =>
      vhs.updateRecord(recorderWorkEntry.id)(
        ifNotExisting = (recorderWorkEntry, EmptyMetadata()))((_, _) =>
        throw new RuntimeException("Not possible, VHS is empty!"))
    })
  }

  def recorderWorkEntryWith(title: String,
                            identifierType: String,
                            sourceId: String,
                            version: Int) = {
    val recorderWorkEntry1 = RecorderWorkEntry(
      UnidentifiedWork(
        title = Some(title),
        SourceIdentifier(IdentifierType(identifierType), "Work", sourceId),
        version = version))
    recorderWorkEntry1
  }

  def getWorksSent(topic: Topic) = {
    val messagesSent = listMessagesReceivedFromSNS(topic)
    val worksSent = messagesSent.map { message =>
      get[UnidentifiedWork](message)
    }
    worksSent
  }
}
