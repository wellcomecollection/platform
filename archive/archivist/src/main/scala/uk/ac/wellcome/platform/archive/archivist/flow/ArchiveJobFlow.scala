package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveJob}
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  IngestBagRequest
}

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String,
            parallelism: Int,
            ingestBagRequest: IngestBagRequest)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob,
           Either[ArchiveError[_], ArchiveComplete],
           NotUsed] =
    Flow[ArchiveJob]
      .log("archive job")
      .via(ArchiveTagManifestFlow(parallelism))
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveJob],
          ArchiveJob,
          Either[ArchiveError[ArchiveJob], ArchiveComplete]](
          OnErrorFlow()
        )(
          ifRight =
            ArchiveJobDigestItemsFlow(delimiter, parallelism, ingestBagRequest)
        ))
}
