package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.storage.ObjectLocation

case class IngestBagRequest(
  id: UUID,
  zippedBagLocation: ObjectLocation,
  archiveCompleteCallbackUrl: Option[URI] = None,
  storageSpace: StorageSpace
)
