package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  FileDownloadComplete,
  IngestBagRequest,
  Parallelism
}

object ArchiveJobFlow extends Logging {

  type ArchiveJobStep = Either[ArchiveError[_], ArchiveJob]
  type CompletedArchiveJob = Either[ArchiveError[_], ArchiveComplete]

  def apply(
    delimiter: String,
    ingestBagRequest: IngestBagRequest
  )(
    implicit s3Client: AmazonS3,
    parallelism: Parallelism
  ): Flow[ArchiveJobStep, CompletedArchiveJob, NotUsed] = {

    val archiveJobDigestItemsFlow = ArchiveJobDigestItemsFlow(
      delimiter,
      ingestBagRequest
    )

    val archiveTagManifestFlow = ArchiveTagManifestFlow()

    Flow[Either[ArchiveError[_], ArchiveJob]]
      .flatMapMerge(
        parallelism.value, {
          case Left(error: ArchiveError[_]) => Source.single(Left(error))
          case Right(archiveJob) =>
            Source
              .single(archiveJob)
              .via(archiveTagManifestFlow)
              .via(archiveJobDigestItemsFlow)
              .via(
                FoldEitherFlow[
                  ArchiveError[ArchiveJob],
                  ArchiveJob,
                  Either[ArchiveError[ArchiveJob], ArchiveComplete]](
                  OnErrorFlow()
                )(
                  ifRight =
                    ArchiveJobDigestItemsFlow(delimiter, ingestBagRequest)
                ))
        }
      )
  }
}
