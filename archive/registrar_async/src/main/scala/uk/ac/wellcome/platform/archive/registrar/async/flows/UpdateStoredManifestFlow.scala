package uk.ac.wellcome.platform.archive.registrar.async.flows
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext

object UpdateStoredManifestFlow {
  def apply(dataStore: VersionedHybridStore[StorageManifest,
                                            EmptyMetadata,
                                            ObjectStore[StorageManifest]],
            progressSnsConfig: SNSConfig)(implicit snsClient: AmazonSNS,
                                          ec: ExecutionContext) =
    Flow[(StorageManifest, ArchiveComplete)]
      .mapAsync(10) {
        case (manifest, ctx) => updateStoredManifest(dataStore, manifest, ctx)
      }
      .map(
        archiveComplete =>
          ProgressStatusUpdate(
            archiveComplete.archiveRequestId,
            Progress.Completed,
            List(ProgressEvent("Bag registered successfully"))
        ))
      .via(
        SnsPublishFlow[ProgressUpdate](
          snsClient,
          progressSnsConfig,
          Some("registration_complete")))
      .map(_ => ())

  private def updateStoredManifest(
    dataStore: VersionedHybridStore[StorageManifest,
                                    EmptyMetadata,
                                    ObjectStore[StorageManifest]],
    storageManifest: StorageManifest,
    archiveComplete: ArchiveComplete)(implicit ec: ExecutionContext) =
    dataStore
      .updateRecord(storageManifest.id.toString)(
        ifNotExisting = (storageManifest, EmptyMetadata()))(
        ifExisting = (_, _) => (storageManifest, EmptyMetadata())
      )
      .map(_ => archiveComplete)
}
