package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  VHSIndexEntry,
  VersionedHybridStore
}

import scala.concurrent.Future

class DynamoInserter(
  versionedHybridStore: VersionedHybridStore[SierraItemRecord,
                                             EmptyMetadata,
                                             ObjectStore[SierraItemRecord]]) {
  def insertIntoDynamo(
    record: SierraItemRecord): Future[VHSIndexEntry[EmptyMetadata]] =
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
}
