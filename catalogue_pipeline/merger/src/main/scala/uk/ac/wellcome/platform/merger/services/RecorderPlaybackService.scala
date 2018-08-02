package uk.ac.wellcome.platform.merger.services

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.matcher.WorkIdentifier
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContext, Future}

/** Before the matcher/merger, the recorder stores a copy of every
  * transformed work in an instance of the VHS.
  *
  * This class looks up recorded works in the VHS, and returns them
  * so the merger has everything it needs to work with.
  *
  */
class RecorderPlaybackService @Inject() (
  versionedHybridStore: VersionedHybridStore[RecorderWorkEntry,
                                             EmptyMetadata,
                                             ObjectStore[RecorderWorkEntry]],
)(implicit ec: ExecutionContext) extends Logging {

  /** Given a collection of matched identifiers, return all the
    * corresponding works from VHS.
    */
  def fetchAllRecorderWorkEntries(workIdentifiers: List[WorkIdentifier]): Future[List[Option[RecorderWorkEntry]]] = {
    Future.sequence(
      workIdentifiers
        .map { getRecorderEntryForIdentifier }
    )
  }

  /** Retrieve a single work from the recorder table.
    *
    * If the work is present in VHS but has a different version to what
    * we're expecting, this method returns [[None]].
    *
    * If the work is missing from VHS, it throws [[NoSuchElementException]].
    */
  private def getRecorderEntryForIdentifier(
    workIdentifier: WorkIdentifier): Future[Option[RecorderWorkEntry]] = {
    workIdentifier.version match {
      case 0 => Future.successful(None)
      case _ =>
        versionedHybridStore.getRecord(id = workIdentifier.identifier).map {
          case None =>
            throw new NoSuchElementException(
              s"Work ${workIdentifier.identifier} is not in VHS!")
          case Some(record) if record.work.version == workIdentifier.version =>
            Some(record)
          case Some(record) =>
            debug(
              s"VHS version = ${record.work.version}, identifier version = ${workIdentifier.version}, so discarding work")
            None
        }
    }
  }
}
