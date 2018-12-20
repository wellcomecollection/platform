package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{FileDownloadComplete, IngestBagRequest}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object TypeAliases {
  type BagDownload = Either[ArchiveError[IngestBagRequest], FileDownloadComplete]
  type IngestError = ArchiveError[IngestBagRequest]
}
