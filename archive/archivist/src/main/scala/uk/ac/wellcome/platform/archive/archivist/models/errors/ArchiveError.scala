package uk.ac.wellcome.platform.archive.archivist.models.errors
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob}

sealed trait ArchiveError[T]{
  val job: T
}

case class ChecksumNotMatchedOnUploadError(expectedChecksum: String, actualCheckSum: String,job: ArchiveItemJob) extends ArchiveError[ArchiveItemJob]{
  override def toString = s"Calculated checksum $actualCheckSum was different from $expectedChecksum for item ${ job.bagDigestItem.location} on upload"
}

case class ChecksumNotMatchedOnDownloadError(expectedChecksum: String, actualCheckSum: String,job: ArchiveItemJob) extends ArchiveError[ArchiveItemJob]{
  override def toString = s"Calculated checksum $actualCheckSum was different from $expectedChecksum for item ${ job.bagDigestItem.location} on download"
}

case class UploadError(exception:Throwable,job: ArchiveItemJob) extends ArchiveError[ArchiveItemJob]{
  override def toString = s"There was an exception while uploading ${job.bagDigestItem.location} to ${job.uploadLocation}: ${exception.getMessage}"
}

case class DownloadError(exception:Throwable,job: ArchiveItemJob) extends ArchiveError[ArchiveItemJob]{
  override def toString = s"There was an exception while downloading object ${job.uploadLocation}: ${exception.getMessage}"
}

case class FileNotFoundError(job: ArchiveItemJob) extends ArchiveError[ArchiveItemJob] {
  override def toString = s"Failed reading file ${job.bagDigestItem.location} from zip file"
}

case class MissingBagManifestError(path: String, job: ArchiveJob) extends ArchiveError[ArchiveJob] {
  override def toString = s"Missing bag manifest $path from zip file"
}

case class InvalidBagManifestError(job: ArchiveJob, manifestName: String) extends ArchiveError[ArchiveJob] {
  override def toString = s"Invalid bag manifest $manifestName"
}

case class ArchiveJobError(job: ArchiveJob, errors: List[ArchiveError[ArchiveItemJob]]) extends ArchiveError[ArchiveJob]
