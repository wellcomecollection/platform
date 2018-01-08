package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.MergedSierraRecord
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_adapter.dynamo.MergedSierraRecordDao
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.sqs.SQSReaderGracefulException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
  mergedSierraRecordDao: MergedSierraRecordDao,
  metrics: MetricsSender
) extends Logging {

  def update(itemRecord: SierraItemRecord): Future[Unit] = {

    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      mergedSierraRecordDao
        .getRecord(bibId)
        .flatMap {
          case Some(existingMergedSierraRecord) =>
            val mergedRecord =
              ItemLinker.linkItemRecord(existingMergedSierraRecord, itemRecord)
            if (mergedRecord != existingMergedSierraRecord)
              mergedSierraRecordDao.updateRecord(mergedRecord)
            else Future.successful(())
          case None =>
            mergedSierraRecordDao.updateRecord(
              MergedSierraRecord(bibId,
                                 itemData = Map(itemRecord.id -> itemRecord)))
        }
    }

    val unlinkUpdateFutures = itemRecord.unlinkedBibIds.map { unlinkedBibId =>
      mergedSierraRecordDao
        .getRecord(unlinkedBibId)
        .flatMap {
          case Some(record) =>
            val mergedRecord =
              ItemUnlinker.unlinkItemRecord(record, itemRecord)
            if (mergedRecord != record)
              mergedSierraRecordDao.updateRecord(mergedRecord)
            else Future.successful(())
          // In the case we cannot find the bib record
          // assume we're too early and put the message back
          case None =>
            Future.failed(
              SQSReaderGracefulException(
                new RuntimeException(
                  s"Missing Bib record to unlink: $unlinkedBibId.")
              ))
        }
    }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }

}
