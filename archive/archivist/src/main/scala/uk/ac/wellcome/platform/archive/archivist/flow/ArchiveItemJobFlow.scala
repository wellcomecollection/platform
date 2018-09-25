package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob

object ArchiveItemJobFlow extends Logging {
  def apply(delimiter: String)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob], NotUsed] = {
    Flow[ArchiveItemJob]
      .log("uploading and verifying")
      .via(UploadItemFlow())
      .via(FoldEitherFlow[ArchiveItemJob, ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob]](ifLeft = job => Left(job))(
        ifRight = DownloadItemFlow()))
      .log("download verified")
  }
}
