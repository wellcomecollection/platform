package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Zip}
import akka.util.ByteString
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.archivist.util.CompareChecksum

import scala.util.{Failure, Success}



object DownloadItemFlow extends Logging with CompareChecksum {

  def apply()(implicit s3Client: S3Client) = {
    Flow[Either[ArchiveItemJob, ArchiveItemJob]]
      .log("download to verify")
      .flatMapConcat({
        case Right(job) => {

          val (source, _) = s3Client
            .download(
              job.uploadLocation.namespace,
              job.uploadLocation.key
            )

          source
            .via(VerifiedDownloadFlow())
            .map(compare(job.bagDigestItem.checksum))
            .map({
              case Success(_) => Right(job)
              case Failure(_) => Left(job)
            })

        }

        case Left(job) => Source.single(Left(job))
      })
  }

}
