package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveItemJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveJobError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  IngestBagRequest,
  Namespace
}

object ArchiveJobDigestItemsFlow extends Logging {
  def apply(delimiter: String,
            parallelism: Int,
            ingestBagRequest: IngestBagRequest)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob,
           Either[ArchiveError[ArchiveJob], ArchiveComplete],
           NotUsed] =
    Flow[ArchiveJob]
      .log("creating archive item jobs")
      .map(job =>
        ArchiveItemJobCreator.createArchiveDigestItemJobs(job, delimiter))
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveJob],
          List[ArchiveDigestItemJob],
          Either[ArchiveError[ArchiveJob], ArchiveComplete]](OnErrorFlow())(
          mapReduceArchiveItemJobs(delimiter, parallelism, ingestBagRequest)))

  private def mapReduceArchiveItemJobs(delimiter: String,
                                       parallelism: Int,
                                       ingestBagRequest: IngestBagRequest)(
    implicit s3Client: AmazonS3): Flow[List[ArchiveDigestItemJob],
                                       Either[ArchiveJobError, ArchiveComplete],
                                       NotUsed] =
    Flow[List[ArchiveDigestItemJob]]
      .mapConcat(identity)
      .via(ArchiveDigestItemJobFlow(delimiter, parallelism))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.archiveJob
        case Left(error)           => error.t.archiveJob
      })
      .fold((
        Nil: List[ArchiveError[ArchiveDigestItemJob]],
        None: Option[ArchiveJob])) { (accumulator, archiveItemJobResult) =>
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
              archiveRequestId = ingestBagRequest.archiveRequestId,
              space = Namespace(ingestBagRequest.storageSpace.underlying),
              bagLocation = archiveJob.bagLocation
            ))
        case (errors, archiveJob) => Left(ArchiveJobError(archiveJob, errors))
      }
}
