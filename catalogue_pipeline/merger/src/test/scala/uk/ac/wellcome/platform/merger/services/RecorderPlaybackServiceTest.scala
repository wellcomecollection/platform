package uk.ac.wellcome.platform.merger.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkIdentifier
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderPlaybackServiceTest extends FunSpec with Matchers with ScalaFutures with LocalVersionedHybridStore with MergerTestUtils {

  it("fetches a single RecorderWorkEntry") {
    val recorderWorkEntry = createRecorderWorkEntryWith(version = 1)

    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withRecorderVHS(bucket, table) { vhs =>
          storeInVHS(vhs, recorderWorkEntry = recorderWorkEntry)

          val service = new RecorderPlaybackService(vhs)

          whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(recorderWorkEntry))) { result =>
            result shouldBe List(Some(recorderWorkEntry))
          }
        }
      }
    }
  }

  private def getWorkIdentifiers(entries: RecorderWorkEntry*): List[WorkIdentifier] =
    entries
      .map { entry => WorkIdentifier(
        identifier = entry.id,
        version = entry.work.version
      )}
      .toList

  private def withRecorderVHS[R](bucket: Bucket, table: Table)(
    testWith: TestWith[VersionedHybridStore[RecorderWorkEntry,
                                            EmptyMetadata,
                                            ObjectStore[RecorderWorkEntry]], R]): R = {
    withTypeVHS[RecorderWorkEntry, EmptyMetadata, R](bucket, table) {
      vhs => testWith(vhs)
    }
  }
}
