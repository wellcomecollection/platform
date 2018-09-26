package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob}
import cats.implicits._
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator

object ArchiveJobFlow {
  def apply(delimiter: String)(implicit s3Client: AmazonS3): Flow[ArchiveJob, Either[ArchiveJob,ArchiveJob], NotUsed] =
    Flow[ArchiveJob]
    .map(job => ArchiveItemJobCreator.createArchiveItemJobs(job, delimiter))
      .via(FoldEitherFlow[ArchiveJob, List[ArchiveItemJob], Either[ArchiveJob,ArchiveJob]](Left(_))(
        mapReduceArchiveItemJobs(delimiter)))


  private def mapReduceArchiveItemJobs(delimiter: String)(implicit s3Client: AmazonS3) = Flow[List[ArchiveItemJob]]
    .mapConcat(identity)
    .via(ArchiveItemJobFlow(delimiter))
    .groupBy(Int.MaxValue, {
      case Right(archiveItemJob) => archiveItemJob.bagName
      case Left(archiveItemJob) => archiveItemJob.bagName
    })
    .reduce((first, second) => if (first.isLeft) first else second)
    .mergeSubstreams
    .map(either => either.map(_.archiveJob).leftMap(_.archiveJob))
}
