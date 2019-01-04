package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.common.json.{
  URIConverters,
  UUIDConverters
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.platform.archive.common.TemporaryStore
import uk.ac.wellcome.platform.archive.common.errors.FileDownloadingError

case class IngestBagRequest(id: UUID,
                            zippedBagLocation: ObjectLocation,
                            archiveCompleteCallbackUrl: Option[URI] = None,
                            storageSpace: StorageSpace) {
  def toIngestBagJob(implicit s3Client: AmazonS3): IngestBagJob = {
    import TemporaryStore._

    val either = zippedBagLocation.downloadTempFile.toEither

    val bagDownload = either.fold(
      error => Left(FileDownloadingError(this, error)),
      file => Right(FileDownloadComplete(file, this))
    )

    IngestBagJob(this, bagDownload)
  }
}

object IngestBagRequest extends URIConverters with UUIDConverters {}

case class IngestBagJob(
  request: IngestBagRequest,
  bagDownload: Either[FileDownloadingError, FileDownloadComplete])
