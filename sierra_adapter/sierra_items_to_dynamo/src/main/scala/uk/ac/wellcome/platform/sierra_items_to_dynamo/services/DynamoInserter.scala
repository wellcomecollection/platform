package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.syntax._
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.dynamo._
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.type_classes.IdGetter

import scala.concurrent.{ExecutionContext, Future}

class DynamoInserter @Inject()(versionedDao: VersionedDao)(
  implicit ec: ExecutionContext) {

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
