package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.models.Sourced
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
      versionedHybridStore.updateRecord(Sourced.id(sourceName, bibId.withoutCheckDigit))(
        ifNotExisting = (
          SierraTransformable(
            sierraId = bibId,
            itemData = Map(itemRecord.id -> itemRecord)),
          SourceMetadata(sourceName)))(
        ifExisting = (existingSierraTransformable, _) => {
          (
            ItemLinker.linkItemRecord(existingSierraTransformable, itemRecord),
            SourceMetadata(sourceName))
        })
    }

    val unlinkUpdateFutures: Seq[Future[Unit]] =
      itemRecord.unlinkedBibIds.map { unlinkedBibId =>
        versionedHybridStore.updateRecord(
          Sourced.id(sourceName, unlinkedBibId.withoutCheckDigit))(
          ifNotExisting = throw GracefulFailureException(
            new RuntimeException(
              s"Missing Bib record to unlink: $unlinkedBibId")
          ))(
          ifExisting = (record, _) =>
            (
              ItemUnlinker.unlinkItemRecord(record, itemRecord),
              SourceMetadata(sourceName))
        )
      }
    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }
}
