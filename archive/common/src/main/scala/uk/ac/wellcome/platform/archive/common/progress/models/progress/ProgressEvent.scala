package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.time.Instant

case class ProgressEvent(description: String,
                         createdDate: Instant = Instant.now)
