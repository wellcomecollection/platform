package uk.ac.wellcome.platform.merger.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkIdentifier
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class RecorderPlaybackServiceTest extends FunSpec with Matchers with ScalaFutures with LocalVersionedHybridStore with MergerTestUtils {

  it("fetches a single RecorderWorkEntry") {
    val recorderWorkEntry = createRecorderWorkEntryWith(version = 1)

    withRecorderVHS { vhs =>
      storeInVHS(vhs, recorderWorkEntry = recorderWorkEntry)

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(recorderWorkEntry))) { result =>
        result shouldBe List(Some(recorderWorkEntry))
      }
    }
  }

  it("throws an error if asked to fetch a missing entry") {
    val recorderWorkEntry = createRecorderWorkEntryWith(version = 1)

    withRecorderVHS { vhs =>
      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(recorderWorkEntry)).failed) { result =>
        result shouldBe a[NoSuchElementException]
        result.getMessage shouldBe s"Work ${recorderWorkEntry.id} is not in VHS!"
      }
    }
  }

  it("returns None if asked to fetch a RecorderWorkEntry with version 0") {
    val recorderWorkEntry = createRecorderWorkEntryWith(version = 0)

    withRecorderVHS { vhs =>
      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(recorderWorkEntry))) { result =>
        result shouldBe List(None)
      }
    }
  }

  it("returns None if the version in VHS has a higher version") {
    val work = createUnidentifiedWorkWith(version = 2)
    val recorderWorkEntry = RecorderWorkEntry(work)

    val workToStore = createUnidentifiedWorkWith(
      sourceIdentifier = work.sourceIdentifier,
      version = work.version + 1
    )

    withRecorderVHS { vhs =>
      storeInVHS(vhs, recorderWorkEntry = RecorderWorkEntry(workToStore))

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(recorderWorkEntry))) { result =>
        result shouldBe List(None)
      }
    }
  }

  it("gets a mixture of works as appropriate") {
    val workEntriesToFetch = (1 to 3).map { _ => createRecorderWorkEntryWith(version = 1) }

    val outdatedWorks = (4 to 5).map { _ => createUnidentifiedWorkWith(version = 1) }
    val outdatedWorkEntriesToFetch = outdatedWorks.map { work => RecorderWorkEntry(work) }
    val updatedWorks = outdatedWorks.map { work => work.copy(version = work.version + 1) }
    val updatedWorkEntriesToStore = updatedWorks.map { work => RecorderWorkEntry(work) }

    val zeroWorkEntries = (6 to 7).map { _ => createRecorderWorkEntryWith(version = 0) }

    val allWorkEntries = (workEntriesToFetch ++ outdatedWorkEntriesToFetch ++ zeroWorkEntries).toList

    withRecorderVHS { vhs =>
      storeInVHS(
        vhs,
        entries = (workEntriesToFetch ++ updatedWorkEntriesToStore ++ zeroWorkEntries).toList
      )

      val service = new RecorderPlaybackService(vhs)

      whenReady(service.fetchAllRecorderWorkEntries(getWorkIdentifiers(allWorkEntries: _*))) { result =>
        result shouldBe (workEntriesToFetch.map { Some(_) } ++ (4 to 7).map { _ => None }).toList
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

  private def withRecorderVHS[R](
    testWith: TestWith[VersionedHybridStore[RecorderWorkEntry,
                                            EmptyMetadata,
                                            ObjectStore[RecorderWorkEntry]], R]): R = {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[RecorderWorkEntry, EmptyMetadata, R](bucket, table) {
          vhs => testWith(vhs)
        }
      }
    }
  }
}
