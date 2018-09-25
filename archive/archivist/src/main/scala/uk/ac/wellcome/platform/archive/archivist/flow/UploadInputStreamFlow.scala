package uk.ac.wellcome.platform.archive.archivist.flow
import java.io.InputStream

import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob

import scala.util.Success

object UploadInputStreamFlow{
  def apply(job: ArchiveItemJob, checksum: String)(implicit s3Client: AmazonS3) = Flow[InputStream]
    .log("converting inputstream")
    .flatMapConcat { inputStream =>
      StreamConverters.fromInputStream(() => inputStream)
    }
    .via(UploadAndGetChecksumFlow(job.uploadLocation))
    .map {
      case Success(calculatedChecksum) if calculatedChecksum == checksum =>
        Right(job)
      case _ => Left(job)
    }
}
