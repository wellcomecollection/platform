package uk.ac.wellcome.platform.archive.common.generators

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation}

trait ArchiveCompleteGenerators extends RandomThings {
  def createArchiveCompleteWith(
    archiveRequestId: UUID = randomUUID,
    bagLocation: BagLocation
  ): ArchiveComplete =
    ArchiveComplete(
      archiveRequestId = archiveRequestId,
      bagLocation = bagLocation
    )
}
