package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  ArchiveJobError
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  IngestBagRequest
}

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String,
            parallelism: Int,
            ingestBagRequest: IngestBagRequest)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob,
           Either[ArchiveError[ArchiveJob], ArchiveComplete],
           NotUsed] =
    Flow[ArchiveJob]
      .log("creating archive item jobs")
      .map(job => ArchiveItemJobCreator.createArchiveItemJobs(job, delimiter))
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveJob],
          List[ArchiveItemJob],
          Either[ArchiveError[ArchiveJob], ArchiveComplete]](error => {
          warn(s"${error.job} failed creating archive item jobs")
          Left(error)
        })(mapReduceArchiveItemJobs(delimiter, parallelism, ingestBagRequest)))

  private def mapReduceArchiveItemJobs(delimiter: String,
                                       parallelism: Int,
                                       ingestBagRequest: IngestBagRequest)(
    implicit s3Client: AmazonS3): Flow[List[ArchiveItemJob],
                                       Either[ArchiveJobError, ArchiveComplete],
                                       NotUsed] =
    Flow[List[ArchiveItemJob]]
      .mapConcat(identity)
      .via(ArchiveItemJobFlow(delimiter, parallelism))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.archiveJob
        case Left(error)           => error.job.archiveJob
      })
      .fold((Nil: List[ArchiveError[ArchiveItemJob]], None: Option[ArchiveJob])) {
        (accumulator, archiveItemJobResult) =>
          (accumulator, archiveItemJobResult) match {
            case ((errorList, _), Right(archiveItemJob)) =>
              (errorList, Some(archiveItemJob.archiveJob))
            case ((errorList, _), Left(error)) =>
              (error :: errorList, Some(error.job.archiveJob))
          }

      }
      .collect {
        case (events, Some(archiveJob)) => (events, archiveJob)
      }
      .mergeSubstreams
      .map {
        case (Nil, archiveJob) =>
          Right(
            ArchiveComplete(
              archiveJob.bagLocation,
              ingestBagRequest
            ))
        case (errors, archiveJob) => Left(ArchiveJobError(archiveJob, errors))

      }
}
