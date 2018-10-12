package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveJobError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagId,
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
          Either[ArchiveError[ArchiveJob], ArchiveComplete]](OnErrorFlow())(
          mapReduceArchiveItemJobs(delimiter, parallelism, ingestBagRequest)))

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
        case Left(error)           => error.t.archiveJob
      })
      .fold((Nil: List[ArchiveError[ArchiveItemJob]], None: Option[ArchiveJob])) {
        (accumulator, archiveItemJobResult) =>
          (accumulator, archiveItemJobResult) match {
            case ((errorList, _), Right(archiveItemJob)) =>
              (errorList, Some(archiveItemJob.archiveJob))
            case ((errorList, _), Left(error)) =>
              (error :: errorList, Some(error.t.archiveJob))
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
              ingestBagRequest.archiveRequestId,
              BagId(
                ingestBagRequest.storageSpace,
                archiveJob.externalIdentifier
              ),
              archiveJob.bagLocation
            ))
        case (errors, archiveJob) => Left(ArchiveJobError(archiveJob, errors))

      }
}
