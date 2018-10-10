package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.events.{
  BatchExecutor,
  LazyFuture
}
import uk.ac.wellcome.platform.sierra_item_merger.exceptions.SierraItemMergerException
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  HybridRecord,
  VersionedHybridStore
}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             EmptyMetadata,
                                             ObjectStore[SierraTransformable]]
)(implicit ec: ExecutionContext)
    extends Logging {

  def update(itemRecord: SierraItemRecord): Future[Seq[HybridRecord]] = {
    val mergeUpdateFutures: Seq[LazyFuture[HybridRecord]] =
      itemRecord.bibIds.map { bibId =>
        LazyFuture {
          versionedHybridStore
            .updateRecord(id = bibId.withoutCheckDigit)(
              ifNotExisting = (
                SierraTransformable(
                  sierraId = bibId,
                  itemRecords = Map(itemRecord.id -> itemRecord)),
                EmptyMetadata()))(ifExisting =
              (existingTransformable, existingMetadata) => {
                (
                  ItemLinker.linkItemRecord(existingTransformable, itemRecord),
                  existingMetadata)
              })
            .map { case (hybridRecord, _) => hybridRecord }
        }
      }

    val unlinkUpdateFutures: Seq[LazyFuture[HybridRecord]] =
      itemRecord.unlinkedBibIds.map { unlinkedBibId =>
        LazyFuture {
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
            .map { case (hybridRecord, _) => hybridRecord }
        }
      }

    BatchExecutor.execute[HybridRecord](
      mergeUpdateFutures ++ unlinkUpdateFutures)(concurFactor = 5)
  }
}
