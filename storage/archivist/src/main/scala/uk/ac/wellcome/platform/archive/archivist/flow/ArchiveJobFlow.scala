package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases.ArchiveCompletion
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  ReplicationRequest
}

object ArchiveJobFlow extends Logging {
  def apply(parallelism: Int, ingestBagRequest: IngestBagRequest)(
    implicit s3Client: AmazonS3)
    : Flow[ArchiveJob, Either[ArchiveError[_], ReplicationRequest], NotUsed] =
    Flow[ArchiveJob]
      .log("archive job")
      .via(ArchiveTagManifestFlow(parallelism))
      .via(
        FoldEitherFlow[ArchiveError[ArchiveJob], ArchiveJob, ArchiveCompletion](
          OnErrorFlow()
        )(
          ifRight = ArchiveJobDigestItemsFlow(parallelism, ingestBagRequest)
        ))
}
