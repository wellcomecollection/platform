package uk.ac.wellcome.platform.archive.registrar.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.{BagArchiveCompleteNotification, BagLocation}

case class RegisterRequestContext(requestId: UUID,
                                  bagLocation: BagLocation,
                                  callbackUrl: Option[URI] = None)


object RegisterRequestContext {
  def apply(notification: BagArchiveCompleteNotification) =
    new RegisterRequestContext(
      notification.archiveRequestId,
      notification.bagLocation,
      notification.archiveCompleteCallbackUrl)
}
