package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.models.Namespace

case class ProgressCreateRequest(uploadUri: URI,
                                 callbackUri: Option[URI],
                                 space: Namespace)

object ProgressCreateRequest extends URIConverters
