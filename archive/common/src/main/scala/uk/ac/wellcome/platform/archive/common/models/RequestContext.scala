package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

case class RequestContext(requestId: UUID,
                          bagLocation: BagLocation,
                          callbackUrl: Option[URI] = None)

object RequestContext {
  def apply(notification: ArchiveComplete) =
    new RequestContext(
      notification.archiveRequestId,
      notification.bagLocation,
      notification.archiveCompleteCallbackUrl)
}
