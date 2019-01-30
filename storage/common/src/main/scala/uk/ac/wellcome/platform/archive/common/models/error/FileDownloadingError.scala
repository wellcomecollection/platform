package uk.ac.wellcome.platform.archive.common.models.error

import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest

case class FileDownloadingError(t: IngestBagRequest, exception: Throwable)
    extends ArchiveError[IngestBagRequest] {
  override def toString =
    s"Failed downloading file ${t.zippedBagLocation.namespace}/${t.zippedBagLocation.key}: ${exception.getMessage}"
}
