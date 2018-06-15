package uk.ac.wellcome.platform.merger

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import scala.concurrent.ExecutionContext.Implicits.global

class MergerFeatureTest
    extends FunSpec
    with Messaging
    with fixtures.Server
    with ExtendedPatience
    with LocalVersionedHybridStore
with ScalaFutures{
  it("reads matcher result messages off a queue and deletes them") {
    withLocalSnsTopic{ topic =>

    withLocalS3Bucket{ bucket =>
      withLocalDynamoDbTable{ table =>
        withTypeVHS[RecorderWorkEntry, EmptyMetadata, Assertion](bucket,table){vhs=>
    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        withServer(queue, topic, bucket, table) { _ =>

          val unidentifiedWork = UnidentifiedWork(title = Some("dfmsng"), SourceIdentifier(IdentifierType("sierra-system-number"), "Work", "b123456"), version = 1)
          val recorderWorkEntry = RecorderWorkEntry(unidentifiedWork)
          val result = vhs.updateRecord(recorderWorkEntry.id)(ifNotExisting = (recorderWorkEntry, EmptyMetadata()))((_, _) => throw new RuntimeException("Not possible, VHS is empty!"))
          whenReady(result){_ =>
          val matcherResult = MatcherResult(Set(MatchedIdentifiers(
            Set(WorkIdentifier(identifier = recorderWorkEntry.id, version = 1)))))

          val notificationMessage = NotificationMessage(
            MessageId = "MessageId",
            TopicArn = "topic-arn",
            Subject = "subject",
            Message = toJson(matcherResult).get
          )
          sqsClient.sendMessage(queue.url, toJson(notificationMessage).get)

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
            val messagesSent = listMessagesReceivedFromSNS(topic)
            val worksSent = messagesSent.map {message => fromJson[UnidentifiedWork](message.message).get}
            worksSent should contain theSameElementsAs List(unidentifiedWork)
          }
        }
    }
  }}}}}}
}
