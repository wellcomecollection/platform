package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.exceptions.SierraItemMergerException
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             SourceMetadata,
                                             ObjectStore[SierraTransformable]]
)(implicit ec: ExecutionContext)
    extends Logging {

  val sourceName = "sierra"

  def update(itemRecord: SierraItemRecord): Future[Unit] = {

    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      versionedHybridStore
        .updateRecord(id = bibId.withoutCheckDigit)(
          ifNotExisting = (
            SierraTransformable(
              sierraId = bibId,
              itemRecords = Map(itemRecord.id -> itemRecord)),
            SourceMetadata("sierra")))(ifExisting =
          (existingTransformable, existingMetadata) => {
            (
              ItemLinker.linkItemRecord(existingTransformable, itemRecord),
              existingMetadata)
          })
        .map { _ =>
          ()
        }
    }

    val unlinkUpdateFutures: Seq[Future[Unit]] =
      itemRecord.unlinkedBibIds.map { unlinkedBibId =>
        versionedHybridStore
          .updateRecord(id = unlinkedBibId.withoutCheckDigit)(
            ifNotExisting = throw SierraItemMergerException(
              s"Missing Bib record to unlink: $unlinkedBibId")
          )(
            ifExisting = (existingTransformable, existingMetadata) =>
              (
                ItemUnlinker
                  .unlinkItemRecord(existingTransformable, itemRecord),
                existingMetadata)
          )
          .map { _ =>
            ()
          }
      }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }
}
