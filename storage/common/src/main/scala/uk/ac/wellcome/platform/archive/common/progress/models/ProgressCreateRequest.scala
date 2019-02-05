package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI

case class ProgressCreateRequest(uploadUri: URI,
                                 callbackUri: Option[URI],
                                 space: Namespace)
