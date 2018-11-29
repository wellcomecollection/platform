package uk.ac.wellcome.platform.archive.common.generators

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  StorageSpace
}

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
}
