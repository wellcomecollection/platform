package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.sierra_adapter.dynamo.SierraTransformableDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
  sierraTransformableDao: SierraTransformableDao,
  metrics: MetricsSender
) extends Logging {

  def update(itemRecord: SierraItemRecord): Future[Unit] = {

    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      sierraTransformableDao
        .getRecord(bibId)
        .flatMap {
          case Some(existingSierraTransformable) =>
            val mergedRecord =
              ItemLinker.linkItemRecord(existingSierraTransformable,
                                        itemRecord)
            if (mergedRecord != existingSierraTransformable)
              sierraTransformableDao.updateRecord(mergedRecord)
            else Future.successful(())
          case None =>
            sierraTransformableDao.updateRecord(
              SierraTransformable(
                sourceId = bibId,
                itemData = Map(itemRecord.id -> itemRecord)
              )
            )
        }
    }

    val unlinkUpdateFutures = itemRecord.unlinkedBibIds.map { unlinkedBibId =>
      sierraTransformableDao
        .getRecord(unlinkedBibId)
        .flatMap {
          case Some(record) =>
            val mergedRecord =
              ItemUnlinker.unlinkItemRecord(record, itemRecord)
            if (mergedRecord != record)
              sierraTransformableDao.updateRecord(mergedRecord)
            else Future.successful(())
          // In the case we cannot find the bib record
          // assume we're too early and put the message back
          case None =>
            Future.failed(
              GracefulFailureException(
                new RuntimeException(
                  s"Missing Bib record to unlink: $unlinkedBibId.")
              ))
        }
    }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }

}
