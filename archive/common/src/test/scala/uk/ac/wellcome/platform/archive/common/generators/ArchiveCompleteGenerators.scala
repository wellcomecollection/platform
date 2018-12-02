package uk.ac.wellcome.platform.archive.common.generators

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models._

trait ArchiveCompleteGenerators extends RandomThings {
  def createArchiveCompleteWith(
    archiveRequestId: UUID = randomUUID,
    space: StorageSpace = randomStorageSpace,
    bagLocation: BagLocation
  ): ArchiveComplete =
    ArchiveComplete(
      archiveRequestId = archiveRequestId,
      space = space,
      bagLocation = bagLocation
    )

  def createArchiveCompleteWith(
    request: IngestBagRequest,
    bagIdentifier: ExternalIdentifier
  ): ArchiveComplete =
    createArchiveCompleteWith(
      archiveRequestId = request.archiveRequestId,
      space = request.storageSpace,
      bagLocation = BagLocation(
        storageNamespace = request.zippedBagLocation.namespace,
        storagePath = "archive",
        bagPath = BagPath(s"${request.storageSpace}/$bagIdentifier")
      )
    )
}
