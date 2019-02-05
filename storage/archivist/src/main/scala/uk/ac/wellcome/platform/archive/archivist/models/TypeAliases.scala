package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  ReplicationRequest
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object TypeAliases {
  type BagDownload = Either[ArchiveError[_], FileDownloadComplete]
  type ArchiveCompletion = Either[ArchiveError[_], ReplicationRequest]

  type IngestError = ArchiveError[IngestBagRequest]
}
