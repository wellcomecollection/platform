package uk.ac.wellcome.platform.archive.registrar.async.models
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.BagId

case class RegistrationCompleteNotification(archiveRequestId: UUID,
                                            bagId: BagId)
