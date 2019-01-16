package uk.ac.wellcome.platform.sierra_item_merger.services

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.exceptions.SierraItemMergerException
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  VHSIndexEntry,
  VersionedHybridStore
}
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerUpdaterService(
  versionedHybridStore: VersionedHybridStore[SierraTransformable,
                                             EmptyMetadata,
                                             ObjectStore[SierraTransformable]]
)(implicit ec: ExecutionContext) {

  def update(itemRecord: SierraItemRecord)
    : Future[List[VHSIndexEntry[EmptyMetadata]]] = {
    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      versionedHybridStore
        .updateRecord(id = bibId.withoutCheckDigit)(
          ifNotExisting = (
            SierraTransformable(
              sierraId = bibId,
              itemRecords = Map(itemRecord.id -> itemRecord)),
            EmptyMetadata()))(
          ifExisting = (existingTransformable, existingMetadata) => {
            (
              ItemLinker.linkItemRecord(existingTransformable, itemRecord),
              existingMetadata)
          })
    }

    val unlinkUpdateFutures =
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
      }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures)
  }
}
