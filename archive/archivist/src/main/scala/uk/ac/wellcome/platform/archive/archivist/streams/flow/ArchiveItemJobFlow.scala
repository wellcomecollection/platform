package uk.ac.wellcome.platform.archive.archivist.streams.flow

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, DigestLocation}

object ArchiveItemJobFlow {

  def apply(delimiter: String)(
    implicit
    s3Client: S3Client
  ) = {

    val bagDigestItemFlow = BagDigestItemFlow(delimiter)
    val archiveItemFlow = ArchiveItemFlow()

    Flow[ArchiveJob]
      .mapConcat(archiveJob => DigestLocation
        .create(archiveJob)
        .map(digestLocation =>
          (digestLocation, archiveJob)
        )
      )
      .via(bagDigestItemFlow)
      .log("bag digest item")
      .via(archiveItemFlow)
  }
}

