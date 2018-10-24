package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.{URIConverters, UUIDConverters}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

case class CallbackNotification(id: UUID, callbackUri: URI, payload: Progress)

object CallbackNotification extends URIConverters with UUIDConverters
