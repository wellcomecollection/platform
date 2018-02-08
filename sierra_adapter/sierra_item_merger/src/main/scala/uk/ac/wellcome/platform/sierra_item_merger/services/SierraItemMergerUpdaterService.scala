package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.generic.extras.semiauto.deriveDecoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.VersionUpdater
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.VersionedHybridStore

import uk.ac.wellcome.dynamo._

import uk.ac.wellcome.utils.JsonUtil._

import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore,
  metrics: MetricsSender
) extends Logging {

  implicit val sierraTransformableUpdater =
    new VersionUpdater[SierraTransformable] {
      override def updateVersion(sierraTransformable: SierraTransformable,
                                 newVersion: Int): SierraTransformable = {
        sierraTransformable.copy(version = newVersion)
      }
    }

  def update(itemRecord: SierraItemRecord): Future[Unit] = {
    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      versionedHybridStore.updateRecord("sierra", bibId)(
        ifNotExisting = SierraTransformable(
          sourceId = bibId,
          itemData = Map(itemRecord.id -> itemRecord)
        ))(ifExisting = existingSierraTransformable => {
        ItemLinker.linkItemRecord(
          existingSierraTransformable,
          itemRecord
        )
      })
    }

    val unlinkUpdateFutures: Seq[Future[Unit]] =
      itemRecord.unlinkedBibIds.map { unlinkedBibId =>
        versionedHybridStore.updateRecord("sierra", unlinkedBibId)(
          throw GracefulFailureException(
            new RuntimeException(
              s"Missing Bib record to unlink: $unlinkedBibId")
          )
        )((record: SierraTransformable) =>
          ItemUnlinker.unlinkItemRecord(record, itemRecord))
      }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }

}
