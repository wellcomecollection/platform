package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}
import uk.ac.wellcome.platform.sierra_item_merger.dynamo.MergedSierraRecordDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
  mergedSierraRecordDao: MergedSierraRecordDao,
  metrics: MetricsSender
) extends Logging {

  def update(itemRecord: SierraItemRecord): Future[Unit] =
    mergedSierraRecordDao
      .getRecord(itemRecord.bibIds.head)
      .flatMap {
        case Some(record) =>
          val mergedRecord = record.mergeItemRecord(itemRecord)
          if (mergedRecord != record)
            mergedSierraRecordDao.updateRecord(mergedRecord)
          else Future.successful(())
        case None =>
          mergedSierraRecordDao.updateRecord(
            MergedSierraRecord(id = itemRecord.bibIds.head,
                               itemRecord = itemRecord))
      }

}
