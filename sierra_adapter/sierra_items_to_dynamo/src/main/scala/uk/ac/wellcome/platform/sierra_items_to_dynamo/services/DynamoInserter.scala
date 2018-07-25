package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.syntax._
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.type_classes.IdGetter

import scala.concurrent.{ExecutionContext, Future}

object DynamoInserter {
  implicit val recordNumberFormat =
    DynamoFormat.coercedXmap[SierraRecordNumber, String, IllegalArgumentException](
      SierraRecordNumber
    )(
      _.withoutCheckDigit
    )

  implicit val idGetter: IdGetter[SierraItemRecord] =
    (record: SierraItemRecord) => record.id.withoutCheckDigit

  implicit val updateExpressionGenerator: UpdateExpressionGenerator[SierraItemRecord] =
    (record: SierraItemRecord) => Some(
      set('bibIds -> record.bibIds) and
      set('data -> record.data) and
      set('modifiedDate -> record.modifiedDate) and
      set('unlinkedBibIds -> record.unlinkedBibIds) and
      set('version -> record.version)
    )
}

class DynamoInserter @Inject()(versionedDao: VersionedDao)(
  implicit ec: ExecutionContext) {

  import DynamoInserter._

  def insertIntoDynamo(record: SierraItemRecord): Future[Unit] =
    versionedDao
      .getRecord[SierraItemRecord](record.id.withoutCheckDigit)
      .flatMap {
        case Some(existingRecord) =>
          val mergedRecord = SierraItemRecordMerger
            .mergeItems(existingRecord = existingRecord, updatedRecord = record)
          if (mergedRecord != existingRecord) {
            println(s"@@AWLC got as far as this IF statement")
            versionedDao.updateRecord(mergedRecord)
          } else {
            Future.successful(())
          }
        case None => {
          println(s"@@AWLC existing record is None")
          versionedDao.updateRecord(record)
        }
      }
}
