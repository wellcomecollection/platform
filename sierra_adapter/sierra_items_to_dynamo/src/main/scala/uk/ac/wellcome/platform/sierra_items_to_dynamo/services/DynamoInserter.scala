package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.SierraItemRecordDao
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

class DynamoInserter(sierraItemRecordDao: SierraItemRecordDao) {

  def insertIntoDynamo(record: SierraItemRecord): Future[Unit] = {
    sierraItemRecordDao.getItem(record.id).flatMap {
      case Some(existingRecord) =>
        val mergedRecord = SierraItemRecordMerger
          .mergeItems(oldRecord = existingRecord, newRecord = record)
        if (mergedRecord != existingRecord) {
          sierraItemRecordDao.updateItem(mergedRecord)
        } else {
          Future.successful(())
        }
      case None => sierraItemRecordDao.updateItem(record)
    }
  }

}
