package uk.ac.wellcome.platform.archive.registrar.flows
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.platform.archive.registrar.models.{RegistrationComplete, StorageManifest}

object NotifyDDSFlow extends Logging {

  def apply(snsConfig: SNSConfig)(implicit snsClient: AmazonSNS) =
    Flow[(StorageManifest, ArchiveComplete)]
      .flatMapConcat({
        case (manifest, archiveComplete) =>
          Source
            .single(RegistrationComplete(archiveComplete.archiveRequestId, manifest))
            .log("notification serialised")
            .via(SnsPublishFlow(snsClient,snsConfig, Some("registrar")))
            .map(_ => archiveComplete)
      })
}
