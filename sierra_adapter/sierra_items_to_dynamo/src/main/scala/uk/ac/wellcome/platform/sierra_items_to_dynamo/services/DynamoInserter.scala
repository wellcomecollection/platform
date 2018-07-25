package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.google.inject.Inject
import com.gu.scanamo.syntax._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.type_classes.IdGetter

import scala.concurrent.{ExecutionContext, Future}

class DynamoInserter @Inject()(versionedDao: VersionedDao)(
  implicit ec: ExecutionContext) {

  implicit val idGetter: IdGetter[SierraItemRecord] =
    (record: SierraItemRecord) => record.id.withoutCheckDigit

  implicit val updateExpressionGenerator: UpdateExpressionGenerator[SierraItemRecord] =
    (record: SierraItemRecord) => Some(
      set('id -> record.id.withoutCheckDigit) and
      set('bibIds -> record.bibIds.map { _.withoutCheckDigit}) and
      set('data -> record.data) and
      set('modifiedDate -> record.modifiedDate) and
      set('unlinkedBibIds -> record.unlinkedBibIds.map { _.withoutCheckDigit }) and
      set('version -> record.version)
    )

  def insertIntoDynamo(record: SierraItemRecord): Future[Unit] = {
    versionedDao
      .getRecord[SierraItemRecord](record.id.withoutCheckDigit)
      .flatMap {
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
