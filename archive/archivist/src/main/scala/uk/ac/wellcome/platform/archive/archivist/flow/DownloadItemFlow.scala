package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob

import scala.util.Try


object DownloadItemFlow extends Logging {

  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob], NotUsed] = {
    Flow[ArchiveItemJob]
      .log("download to verify")
      .flatMapMerge(parallelism, { job =>

          val triedInputStream = Try(s3Client.getObject(job.uploadLocation.namespace, job.uploadLocation.key).getObjectContent)

          triedInputStream.map {inputStream =>

            val downloadSource = StreamConverters
              .fromInputStream(() => inputStream)

            downloadSource
              .via(VerifiedDownloadFlow())
              .map {
                case calculatedChecksum if job.bagDigestItem.checksum == calculatedChecksum => Right(job)
                case _ => Left(job)
              }

          }.getOrElse(Source.single(Left(job)))
      }).async
  }

}
