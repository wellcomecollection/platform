package uk.ac.wellcome.platform.archive.registrar.async.models
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagId,
  BagLocation
}

object ArchiveCompleteGenerator extends RandomThings {

  def createWith(
    bagLocation: BagLocation,
    archiveRequestId: UUID = randomUUID,
    bagId: BagId = randomBagId,
  ) =
    ArchiveComplete(
      archiveRequestId,
      bagId,
      bagLocation
    )
}
