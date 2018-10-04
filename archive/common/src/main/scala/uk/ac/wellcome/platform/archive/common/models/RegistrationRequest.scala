package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

case class RegistrationRequest(requestId: UUID,
                               bagLocation: BagLocation,
                               callbackUrl: Option[URI] = None)

object RegistrationRequest {
  def apply(notification: ArchiveComplete) =
    new RegistrationRequest(
      notification.archiveRequestId,
      notification.bagLocation,
      notification.archiveCompleteCallbackUrl)
}
