package uk.ac.wellcome.platform.archive.registrar.async.flows
import java.util.UUID

import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.models.bagit.BagId
import uk.ac.wellcome.platform.archive.common.progress.models._
import uk.ac.wellcome.platform.archive.registrar.async.models.BagManifestUpdate

import scala.concurrent.{ExecutionContext, Future}

object UpdateStoredManifestFlow {
  def apply(dataStore: VersionedHybridStore[StorageManifest,
                                            EmptyMetadata,
                                            ObjectStore[StorageManifest]],
            progressSnsConfig: SNSConfig)(implicit snsClient: AmazonSNS,
                                          ec: ExecutionContext) =
    Flow[(StorageManifest, BagManifestUpdate)]
      .mapAsync(10) {
        case (manifest, ctx) => updateStoredManifest(dataStore, manifest, ctx)
      }
      .map {
        case (requestId, bagId) =>
          ProgressStatusUpdate(
            requestId,
            Progress.Completed,
            Some(bagId),
            List(ProgressEvent("Bag registered successfully"))
          )
      }
      .via(
        SnsPublishFlow[ProgressUpdate](
          snsClient,
          progressSnsConfig,
          subject = "registration_complete"))
      .map(_ => ())

  private def updateStoredManifest(
    dataStore: VersionedHybridStore[StorageManifest,
                                    EmptyMetadata,
                                    ObjectStore[StorageManifest]],
    storageManifest: StorageManifest,
    bagManifestUpdate: BagManifestUpdate)(
    implicit ec: ExecutionContext): Future[(UUID, BagId)] =
    dataStore
      .updateRecord(storageManifest.id.toString)(
        ifNotExisting = (storageManifest, EmptyMetadata()))(
        ifExisting = (_, _) => (storageManifest, EmptyMetadata())
      )
      .map { _ =>
        (bagManifestUpdate.archiveRequestId, storageManifest.id)
      }
}
