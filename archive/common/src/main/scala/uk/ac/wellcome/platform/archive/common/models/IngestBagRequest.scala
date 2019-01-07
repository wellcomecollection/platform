package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import com.amazonaws.services.s3.transfer.TransferManager
import uk.ac.wellcome.platform.archive.common.json.{
  URIConverters,
  UUIDConverters
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.platform.archive.common.TemporaryStore
import uk.ac.wellcome.platform.archive.common.errors.FileDownloadingError

import scala.concurrent.{ExecutionContext, Future}

case class IngestBagRequest(id: UUID,
                            zippedBagLocation: ObjectLocation,
                            archiveCompleteCallbackUrl: Option[URI] = None,
                            storageSpace: StorageSpace) {
  def toIngestBagJob(implicit transferManager: TransferManager,
                     ec: ExecutionContext): IngestBagJob = {
    import TemporaryStore._

    val bagDownload = zippedBagLocation.downloadTempFile.map {
      file => Right(FileDownloadComplete(file, this))}
        .recover{case error: Throwable => Left(FileDownloadingError(this, error))}

    IngestBagJob(this, bagDownload)
  }
}

object IngestBagRequest extends URIConverters with UUIDConverters {}

case class IngestBagJob(
  request: IngestBagRequest,
  bagDownload: Future[Either[FileDownloadingError, FileDownloadComplete]])
