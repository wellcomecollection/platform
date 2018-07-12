package uk.ac.wellcome.platform.merger

import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MergerTestUtils extends WorksUtil { this: SQS with SNS with Messaging =>

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
                 recorderWorkEntry: RecorderWorkEntry): Future[Unit] =
    vhs.updateRecord(recorderWorkEntry.id)(
      ifNotExisting = (recorderWorkEntry, EmptyMetadata()))((_, _) =>
      throw new RuntimeException("Not possible, VHS is empty!"))

  def storeInVHS(vhs: VersionedHybridStore[RecorderWorkEntry,
                                           EmptyMetadata,
                                           ObjectStore[RecorderWorkEntry]],
                 entries: List[RecorderWorkEntry]): Future[List[Unit]] = {
    Future.sequence(entries.map { recorderWorkEntry =>
      storeInVHS(vhs = vhs, recorderWorkEntry = recorderWorkEntry)
    })
  }

  def createRecorderWorkEntryWith(version: Int) =
    RecorderWorkEntry(createUnidentifiedWorkWith(version = version))
}
