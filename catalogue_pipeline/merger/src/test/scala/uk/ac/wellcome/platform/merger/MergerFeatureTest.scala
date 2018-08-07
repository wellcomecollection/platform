package uk.ac.wellcome.platform.merger

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class MergerFeatureTest
    extends FunSpec
    with Messaging
    with fixtures.Server
    with ExtendedPatience
    with LocalVersionedHybridStore
    with ScalaFutures
    with MergerTestUtils {

  it("reads matcher result messages off a queue and deletes them") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalDynamoDbTable { table =>
            withTypeVHS[RecorderWorkEntry, EmptyMetadata, Assertion](
              storageBucket,
              table) { vhs =>
              withLocalSqsQueueAndDlq {
                case QueuePair(queue, dlq) =>
                  withServer(
                    queue = queue,
                    topic = topic,
                    storageBucket = storageBucket,
                    messageBucket = messagesBucket,
                    table = table) { _ =>
                    val recorderWorkEntry =
                      createRecorderWorkEntryWith(version = 1)

                    storeInVHS(vhs, recorderWorkEntry)

                    val matcherResult =
                      matcherResultWith(Set(Set(recorderWorkEntry)))
                    sendNotificationToSQS(queue, matcherResult)

                    eventually {
                      assertQueueEmpty(queue)
                      assertQueueEmpty(dlq)
                      val worksSent = getMessages[TransformedBaseWork](topic)
                      worksSent should contain only recorderWorkEntry.work
                    }
                  }
              }
            }
          }
        }
      }
    }
  }
}
