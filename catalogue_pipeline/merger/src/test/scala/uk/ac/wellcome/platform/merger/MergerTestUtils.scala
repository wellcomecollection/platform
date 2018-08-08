package uk.ac.wellcome.platform.merger

import org.scalatest.{Assertion, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier
}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

trait MergerTestUtils
    extends Eventually
    with ScalaFutures
    with LocalVersionedHybridStore
    with WorksUtil { this: Suite =>

  def matcherResultWith(matchedEntries: Set[Set[RecorderWorkEntry]]) =
    MatcherResult(
      matchedEntries.map(
        recorderWorkEntries =>
          MatchedIdentifiers(
            recorderWorkEntries.map(workEntry =>
              WorkIdentifier(
                identifier = workEntry.id,
                version = workEntry.work.version)))))

  def storeInVHS(vhs: VersionedHybridStore[RecorderWorkEntry,
                                           EmptyMetadata,
                                           ObjectStore[RecorderWorkEntry]],
                 recorderWorkEntry: RecorderWorkEntry): Assertion = {
    vhs.updateRecord(recorderWorkEntry.id)(
      ifNotExisting = (recorderWorkEntry, EmptyMetadata()))((_, _) =>
      throw new RuntimeException("Not possible, VHS is empty!"))

    eventually {
      whenReady(vhs.getRecord(id = recorderWorkEntry.id)) { result =>
        result.get shouldBe recorderWorkEntry
      }
    }
  }

  def storeInVHS(vhs: VersionedHybridStore[RecorderWorkEntry,
                                           EmptyMetadata,
                                           ObjectStore[RecorderWorkEntry]],
                 entries: List[RecorderWorkEntry]): List[Assertion] =
    entries.map { recorderWorkEntry =>
      storeInVHS(vhs = vhs, recorderWorkEntry = recorderWorkEntry)
    }

  def createRecorderWorkEntryWith(version: Int): RecorderWorkEntry =
    RecorderWorkEntry(createUnidentifiedWorkWith(version = version))

  def createRecorderWorkEntry: RecorderWorkEntry =
    createRecorderWorkEntryWith(version = 1)
}
