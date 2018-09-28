package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob
}
import cats.implicits._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String, parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob, Either[ArchiveJob, ArchiveJob], NotUsed] =
    Flow[ArchiveJob]
      .log("creating archive item jobs")
      .map(job => ArchiveItemJobCreator.createArchiveItemJobs(job, delimiter))
      .via(
        FoldEitherFlow[
          ArchiveJob,
          List[ArchiveItemJob],
          Either[ArchiveJob, ArchiveJob]](job => {
          warn(s"$job failed creating archive item jobs")
          Left(job)
        })(mapReduceArchiveItemJobs(delimiter, parallelism)))

  private def mapReduceArchiveItemJobs(delimiter: String, parallelism: Int)(
    implicit s3Client: AmazonS3) =
    Flow[List[ArchiveItemJob]]
      .mapConcat(identity)
      .via(ArchiveItemJobFlow(delimiter, parallelism))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.bagName
        case Left(archiveItemJob)  => archiveItemJob.bagName
      })
      .reduce((first, second) => if (first.isLeft) first else second)
      .mergeSubstreams
      .map(either => either.map(_.archiveJob).leftMap(_.archiveJob))
}
