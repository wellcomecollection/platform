package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  FileDownloadComplete,
  IngestBagRequest
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object ArchiveAndNotifyRegistrarFlow {

  type BagDownload =
    Either[ArchiveError[IngestBagRequest], FileDownloadComplete]

  def apply(bagUploaderConfig: BagUploaderConfig,
            snsProgressConfig: SNSConfig,
            snsRegistrarConfig: SNSConfig)(
    implicit s3: AmazonS3,
    snsClient: AmazonSNS
  ): Flow[BagDownload, Unit, NotUsed] = {
    ArchiveZipFileFlow(bagUploaderConfig, snsProgressConfig)
      .via(
        FoldEitherFlow[
          ArchiveError[_],
          ArchiveComplete,
          Unit
        ](ifLeft = Flow[ArchiveError[_]].map(_ => ()))(ifRight =
          RegistrarNotifierFlow(snsRegistrarConfig, snsProgressConfig).map(_ =>
            ())))
  }
}
