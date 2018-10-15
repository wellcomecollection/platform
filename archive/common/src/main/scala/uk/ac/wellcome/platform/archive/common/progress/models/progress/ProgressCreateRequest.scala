package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.models.StorageSpace

case class ProgressCreateRequest(uploadUri: URI, callbackUri: Option[URI], space: StorageSpace)

object ProgressCreateRequest extends URIConverters
