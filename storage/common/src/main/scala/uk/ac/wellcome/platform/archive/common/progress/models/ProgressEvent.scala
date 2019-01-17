package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant

case class ProgressEvent(description: String,
                         createdDate: Instant = Instant.now)
