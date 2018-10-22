package uk.ac.wellcome.platform.archive.archivist.models.errors
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

case class ChecksumNotMatchedOnUploadError(expectedChecksum: String,
                                           actualCheckSum: String,
                                           t: ArchiveItemJob)
    extends ArchiveError[ArchiveItemJob] {
  override def toString =
    s"Calculated checksum $actualCheckSum was different from $expectedChecksum for item ${t.bagDigestItem.location} on upload"
}

case class ChecksumNotMatchedOnDownloadError(expectedChecksum: String,
                                             actualCheckSum: String,
                                             t: ArchiveItemJob)
    extends ArchiveError[ArchiveItemJob] {
  override def toString =
    s"Calculated checksum $actualCheckSum was different from $expectedChecksum for item ${t.bagDigestItem.location} on download"
}

case class UploadError(exception: Throwable, t: ArchiveItemJob)
    extends ArchiveError[ArchiveItemJob] {
  override def toString =
    s"There was an exception while uploading ${t.bagDigestItem.location} to ${t.uploadLocation}: ${exception.getMessage}"
}

case class FileNotFoundError[T](path: String, t: T) extends ArchiveError[T] {
  override def toString = s"Failed reading file $path from zip file"
}

case class ArchiveJobError(t: ArchiveJob,
                           errors: List[ArchiveError[ArchiveItemJob]])
    extends ArchiveError[ArchiveJob]

case class InvalidBagInfo[T](t: T)
    extends ArchiveError[T] {
  override def toString = "Invalid bag-info.txt"
}

case class ZipFileDownloadingError(t: IngestBagRequest, exception: Throwable)
    extends ArchiveError[IngestBagRequest] {
  override def toString =
    s"Failed downloading zipFile ${t.zippedBagLocation.namespace}/${t.zippedBagLocation.key}: ${exception.getMessage}"
}
