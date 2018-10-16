package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import uk.ac.wellcome.platform.archive.common.json.URIConverters

case class ProgressCreateRequest(uploadUri: URI,
                                 callbackUri: Option[URI],
                                 space: Namespace)

object ProgressCreateRequest extends URIConverters
