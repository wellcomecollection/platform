package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressEvent

object UploadItemFlow extends Logging {
  def apply(parallelism: Int)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[(ProgressEvent,ArchiveItemJob), ArchiveItemJob], NotUsed] = {

    Flow[ArchiveItemJob]
      .map(j => (j, ZipFileReader.maybeInputStream(ZipLocation(j))))
      .map {
        case (j, option) =>
          option.toRight(j).map(inputStream => (j, inputStream))
      }
      .via(
        FoldEitherFlow[
          ArchiveItemJob,
          (ArchiveItemJob, InputStream),
          Either[(ProgressEvent,ArchiveItemJob), ArchiveItemJob]](j => {
          warn(s"Failed extracting inputStream for $j")
          Left((ProgressEvent(s"Failed reading file ${j.bagDigestItem.location} from zip file"),j))
        })(UploadInputStreamFlow(parallelism)))
  }

}
