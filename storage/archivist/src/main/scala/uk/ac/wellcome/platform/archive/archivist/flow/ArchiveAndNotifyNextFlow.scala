package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases.BagDownload
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.models.ReplicationRequest
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object ArchiveAndNotifyNextFlow {
  def apply(bagUploaderConfig: BagUploaderConfig,
            snsProgressConfig: SNSConfig,
            snsNextConfig: SNSConfig)(
    implicit s3: AmazonS3,
    snsClient: AmazonSNS
  ): Flow[BagDownload, Unit, NotUsed] = {

    val nextNotifierFlow = NextNotifierFlow(
      snsNextConfig,
      snsProgressConfig
    ).map(_ => ())

    val unitFlow = Flow[ArchiveError[_]].map(_ => ())

    ArchiveZipFileFlow(bagUploaderConfig, snsProgressConfig)
      .via(
        FoldEitherFlow[
          ArchiveError[_],
          ReplicationRequest,
          Unit
        ](
          ifLeft = unitFlow
        )(
          ifRight = nextNotifierFlow
        )
      )
  }
}
