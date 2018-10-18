package uk.ac.wellcome.platform.archive.registrar.async.flows
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.platform.archive.registrar.async.models.RegistrationCompleteNotification
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.json.JsonUtil._

object NotifyDDSFlow extends Logging {

  def apply(snsConfig: SNSConfig)(implicit snsClient: AmazonSNS) =
    Flow[(StorageManifest, ArchiveComplete)]
      .flatMapConcat({
        case (manifest, archiveComplete) =>
          Source
            .single(
              RegistrationCompleteNotification(
                archiveComplete.archiveRequestId,
                manifest.id))
            .log("notification serialised")
            .via(SnsPublishFlow(snsClient, snsConfig, Some("registrar")))
            .map(_ => archiveComplete)
      })
}
