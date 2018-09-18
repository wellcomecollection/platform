package uk.ac.wellcome.platform.archive.archivist.streams.flow

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob
import uk.ac.wellcome.platform.archive.archivist.streams.source.ArchiveBagSource

object ArchiveBagFlow {
  def apply()(implicit s3Client: S3Client): Flow[ArchiveJob, ArchiveJob, NotUsed] =
    Flow[ArchiveJob]
      .flatMapConcat(ArchiveBagSource(_))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.bagName
        case Left(archiveItemJob) => archiveItemJob.bagName
      })
      .reduce((first, second) => if (first.isLeft) first else second)
      .mergeSubstreams
      .collect {
        case Right(archiveItemJob) => archiveItemJob.archiveJob
      }
}
