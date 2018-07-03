package uk.ac.wellcome.platform.recorder

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderFeatureTest
    extends FunSpec
    with Matchers
    with ExtendedPatience
    with fixtures.Server
    with LocalVersionedHybridStore
    with Messaging
    with WorksUtil {

  it("receives a transformed Work, and saves it to the VHS") {
    val work = createUnidentifiedWork

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withTypeVHS[RecorderWorkEntry, EmptyMetadata, Assertion](
            bucket = bucket,
            table = table) { _ =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table) ++ messageReaderLocalFlags(
              bucket,
              queue)
            withServer(flags) { _ =>
              val messageBody = put[TransformedBaseWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = "work_message.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                assertStored[RecorderWorkEntry](
                  bucket,
                  table,
                  RecorderWorkEntry(work))
              }
            }
          }
        }
      }
    }
  }
}
