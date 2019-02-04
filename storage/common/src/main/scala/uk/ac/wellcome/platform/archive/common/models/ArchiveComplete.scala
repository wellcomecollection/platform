package uk.ac.wellcome.platform.archive.common.models

import java.util.UUID

case class ArchiveComplete(
  archiveRequestId: UUID,
  space: StorageSpace,
  bagLocation: BagLocation
)
