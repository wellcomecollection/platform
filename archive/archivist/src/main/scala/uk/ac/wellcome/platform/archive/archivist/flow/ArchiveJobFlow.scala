package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob}
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressEvent

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String, parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob, Either[(List[ProgressEvent],ArchiveJob), ArchiveJob], NotUsed] =
    Flow[ArchiveJob]
      .log("creating archive item jobs")
      .map(job => ArchiveItemJobCreator.createArchiveItemJobs(job, delimiter))
      .via(
        FoldEitherFlow[
          (ProgressEvent, ArchiveJob),
          List[ArchiveItemJob],
          Either[(List[ProgressEvent],ArchiveJob), ArchiveJob]]{ case (event, job) =>
              warn(s"$job failed creating archive item jobs")
          Left((List(event),job))}(mapReduceArchiveItemJobs(delimiter, parallelism)))

  private def mapReduceArchiveItemJobs(delimiter: String, parallelism: Int)(
    implicit s3Client: AmazonS3): Flow[List[ArchiveItemJob], Either[(List[ProgressEvent], ArchiveJob),ArchiveJob], NotUsed] =
    Flow[List[ArchiveItemJob]]
      .mapConcat(identity)
      .via(ArchiveItemJobFlow(delimiter, parallelism))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.bagName
        case Left((_,archiveItemJob))  => archiveItemJob.bagName
      })
    .fold((Nil: List[ProgressEvent], None: Option[ArchiveJob])){(tuple, archiveItemJobResult) =>
      (tuple, archiveItemJobResult) match {
        case ((eventList, _),Right(archiveItemJob)) => (eventList, Some(archiveItemJob.archiveJob))
        case ((eventList, _),Left((event, archiveItemJob))) => (event :: eventList, Some(archiveItemJob.archiveJob))
      }
    }.collect {
      case (events, Some(archiveJob)) => (events, archiveJob)
    }
      .mergeSubstreams
    .map {
      case (events, archiveJob) if events.nonEmpty => Left((events, archiveJob))
      case (_, archiveJob) => Right(archiveJob)
     }
}
