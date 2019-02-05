package uk.ac.wellcome.platform.archive.common.models

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.bagit.BagLocation

case class ArchiveComplete(
  archiveRequestId: UUID,
  bagLocation: BagLocation
)

// TODO: Agree upon these names
case class ReplicationRequest(
  archiveRequestId: UUID,
  srcBagLocation: BagLocation
)

case class ReplicationResult(
  archiveRequestId: UUID,
  srcBagLocation: BagLocation
)
