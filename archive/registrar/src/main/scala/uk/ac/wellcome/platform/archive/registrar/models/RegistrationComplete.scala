package uk.ac.wellcome.platform.archive.registrar.models

import java.util.UUID

case class BagRegistrationCompleteNotification(archiveRequestId: UUID,
                                               storageManifest: BagManifest)
