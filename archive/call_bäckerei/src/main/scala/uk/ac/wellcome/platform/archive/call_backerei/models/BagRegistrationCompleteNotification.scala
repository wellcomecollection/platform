package uk.ac.wellcome.platform.archive.call_backerei.models

import java.util.UUID

case class BagRegistrationCompleteNotification(archiveRequestId: UUID,
                                               storageManifest: StorageManifest)
