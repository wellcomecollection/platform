package uk.ac.wellcome.platform.archive.registrar.models

import java.util.UUID

case class RegistrationComplete(archiveRequestId: UUID,
                                               storageManifest: StorageManifest)
