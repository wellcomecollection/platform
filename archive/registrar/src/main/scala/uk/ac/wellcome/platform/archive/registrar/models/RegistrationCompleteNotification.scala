package uk.ac.wellcome.platform.archive.registrar.models

import java.util.UUID

case class RegistrationCompleteNotification(archiveRequestId: UUID,
                                            bagId: BagId)
