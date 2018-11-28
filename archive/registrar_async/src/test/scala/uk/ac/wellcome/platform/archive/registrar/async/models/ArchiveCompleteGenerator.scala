package uk.ac.wellcome.platform.archive.registrar.async.models
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagLocation,
  Namespace
}

object ArchiveCompleteGenerator extends RandomThings {
  def createWith(
    bagLocation: BagLocation,
    archiveRequestId: UUID = randomUUID,
    space: Namespace = createNamespace,
  ): ArchiveComplete =
    ArchiveComplete(
      archiveRequestId = archiveRequestId,
      space = space,
      bagLocation = bagLocation
    )
}
