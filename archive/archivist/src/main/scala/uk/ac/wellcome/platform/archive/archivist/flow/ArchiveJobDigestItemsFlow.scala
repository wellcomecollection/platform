package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
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
  Parallelism
}

object ArchiveJobDigestItemsFlow extends Logging {
  type ArchiveJobStep = Either[ArchiveError[_], ArchiveJob]
  type ArchiveCompletion = Either[ArchiveError[_], ArchiveComplete]

  def apply(
    delimiter: String,
    ingestBagRequest: IngestBagRequest
  )(
    implicit s3Client: AmazonS3,
    parallelism: Parallelism
  ): Flow[ArchiveJobStep, ArchiveCompletion, NotUsed] =
    Flow[ArchiveJobStep]
      .flatMapMerge(
        parallelism.value, {
          case Left(error: ArchiveError[_]) => Source.single(Left(error))
          case Right(archiveJob) =>
            Source
              .single(archiveJob)
              .map(
                ArchiveItemJobCreator.createArchiveDigestItemJobs(_, delimiter))
              .via(
                FoldEitherFlow[
                  ArchiveError[ArchiveJob],
                  List[ArchiveDigestItemJob],
                  Either[ArchiveError[ArchiveJob], ArchiveComplete]](
                  OnErrorFlow())(
                  mapReduceArchiveItemJobs(
                    delimiter,
                    parallelism.value,
                    ingestBagRequest)))
        }
      )

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
              ingestBagRequest.id,
              ingestBagRequest.storageSpace,
              archiveJob.bagLocation
            ))
        case (errors, archiveJob) => Left(ArchiveJobError(archiveJob, errors))
      }
}
