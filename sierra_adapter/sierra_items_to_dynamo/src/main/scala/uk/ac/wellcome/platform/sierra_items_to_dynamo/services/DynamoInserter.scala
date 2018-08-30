package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.google.inject.Inject
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  HybridRecord,
  VersionedHybridStore
}

import scala.concurrent.{ExecutionContext, Future}

class DynamoInserter @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraItemRecord,
                                             EmptyMetadata,
                                             ObjectStore[SierraItemRecord]])(
  implicit ec: ExecutionContext) {

  def insertIntoDynamo(record: SierraItemRecord): Future[HybridRecord] =
    versionedHybridStore
      .updateRecord(
        id = record.id.withoutCheckDigit
      )(
        ifNotExisting = (record, EmptyMetadata())
      )(
        ifExisting = (existingRecord, existingMetadata) =>
          (
            SierraItemRecordMerger
              .mergeItems(
                existingRecord = existingRecord,
                updatedRecord = record),
            existingMetadata
        )
      )
      .map { case (hybridRecord, _) => hybridRecord }
}
