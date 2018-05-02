package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.google.inject.Inject
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

class DynamoInserter @Inject()(versionedDao: VersionedDao) {

  def insertIntoDynamo(record: SierraItemRecord): Future[Unit] = {
    versionedDao.getRecord[SierraItemRecord](record.id).flatMap {
      case Some(existingRecord) =>
        val mergedRecord = SierraItemRecordMerger
          .mergeItems(existingRecord = existingRecord, updatedRecord = record)
        if (mergedRecord != existingRecord) {
          versionedDao.updateRecord(mergedRecord)
        } else {
          Future.successful(())
        }
      case None => versionedDao.updateRecord(record)
    }
  }

}
