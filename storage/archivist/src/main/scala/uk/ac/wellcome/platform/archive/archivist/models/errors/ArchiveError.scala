package uk.ac.wellcome.platform.archive.archivist.models.errors
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ArchiveItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.storage.ObjectLocation

case class ChecksumNotMatchedOnUploadError(expectedChecksum: String,
                                           actualChecksum: String,
                                           t: ArchiveDigestItemJob)
    extends ArchiveError[ArchiveDigestItemJob] {
  override def toString =
    s"Calculated checksum $actualChecksum was different from $expectedChecksum for item ${t.bagDigestItem.path} on upload"
}

case class ChecksumNotMatchedOnDownloadError(expectedChecksum: String,
                                             actualChecksum: String,
                                             t: ArchiveDigestItemJob)
    extends ArchiveError[ArchiveDigestItemJob] {
  override def toString =
    s"Calculated checksum $actualChecksum was different from $expectedChecksum for item ${t.bagDigestItem.path} on download"
}

case class UploadDigestItemError(exception: Throwable, t: ArchiveDigestItemJob)
    extends ArchiveError[ArchiveDigestItemJob] {
  override def toString =
    s"There was an exception while uploading ${t.bagDigestItem.path} to ${t.uploadLocation}: ${exception.getMessage}"
}

case class UploadError[T](objectLocation: ObjectLocation,
                          exception: Throwable,
                          t: T)
    extends ArchiveError[T] {
  override def toString =
    s"There was an exception while uploading to $objectLocation: ${exception.getMessage}"
}

case class FileNotFoundError[T](path: String, t: T) extends ArchiveError[T] {
  override def toString = s"Failed reading file $path from zip file"
}

case class BagNotFoundError[T](message: String, t: T) extends ArchiveError[T] {
  override def toString = s"Failed to identify bag in zip file, $message"
}

case class ArchiveJobError(t: ArchiveJob,
                           errors: List[ArchiveError[ArchiveDigestItemJob]])
    extends ArchiveError[ArchiveJob]

case class ArchiveItemJobError(t: ArchiveJob,
                               errors: List[ArchiveError[ArchiveItemJob]])
    extends ArchiveError[ArchiveJob]
