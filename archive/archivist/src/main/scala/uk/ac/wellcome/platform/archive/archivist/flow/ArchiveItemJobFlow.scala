package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressEvent

object ArchiveItemJobFlow extends Logging {
  def apply(delimiter: String, parallelism: Int)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[(ProgressEvent, ArchiveItemJob), ArchiveItemJob], NotUsed] = {
    Flow[ArchiveItemJob]
      .log("uploading and verifying")
      .via(UploadItemFlow(parallelism))
      .via(
        FoldEitherFlow[
          (ProgressEvent, ArchiveItemJob),
          ArchiveItemJob,
          Either[(ProgressEvent, ArchiveItemJob), ArchiveItemJob]](ifLeft = {case (update, job) =>
              warn(s"job $job uploading and verifying failed")
              Left((update,job))
        })(ifRight = DownloadItemFlow(parallelism)))
      .log("download verified")
  }
}
