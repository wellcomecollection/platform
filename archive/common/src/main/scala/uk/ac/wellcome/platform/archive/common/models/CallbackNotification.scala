package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

case class CallbackNotification(id: UUID, callbackUri: URI, payload: Progress)

object CallbackNotification extends URIConverters {

  def apply(progress: Progress): CallbackNotification = {
    CallbackNotification(
      progress.id,
      progress.callbackUri.getOrElse(
        throw new RuntimeException(
          "Could not createCallbackNotification, no callbackUrl found.")
      ),
      progress
    )
  }

}
