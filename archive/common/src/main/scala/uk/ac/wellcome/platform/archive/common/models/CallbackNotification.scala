package uk.ac.wellcome.platform.archive.common.models

import uk.ac.wellcome.platform.archive.common.progress.models.Progress

case class CallbackNotification(id: String,
                                callbackUrl: String,
                                payload: Progress)

object CallbackNotification {
  def apply(progress: Progress): CallbackNotification = {
    CallbackNotification(
      progress.id,
      progress.callbackUrl.getOrElse(
        throw new RuntimeException(
          "Could not createCallbackNotification, no callbackUrl found.")
      ),
      progress
    )
  }

}
