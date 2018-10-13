package uk.ac.wellcome.platform.archive.registrar.flows
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.registrar.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object UpdateStoredManifestFlow {
  def apply(dataStore: VersionedHybridStore[StorageManifest,
                                            EmptyMetadata,
                                            ObjectStore[StorageManifest]],
            ddsSnsConfig: SNSConfig,
            progressSnsConfig: SNSConfig)(implicit snsClient: AmazonSNS,
                                          ec: ExecutionContext) =
    Flow[(StorageManifest, ArchiveComplete)]
      .mapAsync(10) {
        case (manifest, ctx) => updateStoredManifest(dataStore, manifest, ctx)
      }
      .via(NotifyDDSFlow(ddsSnsConfig))
      .map(
        archiveComplete =>
          ProgressUpdate(
            archiveComplete.archiveRequestId,
            List(ProgressEvent("Bag registered successfully")),
            Progress.Completed))
      .via(
        SnsPublishFlow(
          snsClient,
          progressSnsConfig,
          Some("registration_complete")))
      .map(_ => ())

  private def updateStoredManifest(
    dataStore: VersionedHybridStore[StorageManifest,
                                    EmptyMetadata,
                                    ObjectStore[StorageManifest]],
    storageManifest: StorageManifest,
    requestContext: ArchiveComplete)(implicit ec: ExecutionContext) =
    dataStore
      .updateRecord(storageManifest.id.toString)(
        ifNotExisting = (storageManifest, EmptyMetadata()))(
        ifExisting = (_, _) => (storageManifest, EmptyMetadata())
      )
      .map(_ => (storageManifest, requestContext))
}
