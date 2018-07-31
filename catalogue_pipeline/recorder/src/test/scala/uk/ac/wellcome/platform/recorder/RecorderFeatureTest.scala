package uk.ac.wellcome.platform.recorder

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

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
              sendMessage[TransformedBaseWork](
                bucket = bucket,
                queue = queue,
                obj = work)

              val workEntry = RecorderWorkEntry(work)

              eventually {
                assertStored[RecorderWorkEntry](
                  bucket,
                  table,
                  id = workEntry.id,
                  record = workEntry)
              }
            }
          }
        }
      }
    }
  }
}
