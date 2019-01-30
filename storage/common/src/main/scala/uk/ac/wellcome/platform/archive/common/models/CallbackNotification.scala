package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.progress.models.Progress

case class CallbackNotification(id: UUID, callbackUri: URI, payload: Progress)
