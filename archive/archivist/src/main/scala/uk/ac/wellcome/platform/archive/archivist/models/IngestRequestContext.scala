package uk.ac.wellcome.platform.archive.archivist.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.storage.ObjectLocation

case class IngestRequestContext(requestId: UUID,
                                bagLocation: ObjectLocation,
                                callbackUrl: Option[URI] = None)

object IngestRequestContext {
  def apply(notification: IngestBagRequestNotification) =
    new IngestRequestContext(
      notification.archiveRequestId,
      notification.bagLocation,
      notification.archiveCompleteCallbackUrl)
}
