package uk.ac.wellcome.platform.archive.archivist.streams.source

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, DigestLocation}
import uk.ac.wellcome.platform.archive.archivist.streams.flow.{ArchiveItemFlow, BagDigestItemFlow}

object ArchiveBagSource {

  def apply(archiveJob: ArchiveJob)(
    implicit
    s3Client: S3Client
  ) = {

    val bagDigestItemFlow = BagDigestItemFlow(archiveJob.digestDelimiter)
    val archiveItemFlow = ArchiveItemFlow()
    val digestLocations = DigestLocation.create(archiveJob)

    Source
      .fromIterator(() => digestLocations)
      .map((_, archiveJob.bagLocation.bagPath, archiveJob.zipFile))
      .via(bagDigestItemFlow)
      .log("bag digest item")
      .map(ArchiveItemJob(archiveJob, _))
      .via(archiveItemFlow)
  }
}

