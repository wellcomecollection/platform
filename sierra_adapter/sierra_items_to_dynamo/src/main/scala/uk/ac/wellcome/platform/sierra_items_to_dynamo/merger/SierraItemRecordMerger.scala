package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecordNumber}

object SierraItemRecordMerger {
  def mergeItems(existingRecord: SierraItemRecord,
                 updatedRecord: SierraItemRecord): SierraItemRecord = {

    if (existingRecord.modifiedDate.isBefore(updatedRecord.modifiedDate)) {

      updatedRecord.copy(
        // We always carry across the version from the existing record.
        version = existingRecord.version,
        // Let's suppose we have
        //
        //    oldRecord = (linked = {1, 2, 3}, unlinked = {4, 5})
        //    newRecord = (linked = {3, 4})
        //
        // First we get a list of all the bibIds the oldRecord knew about, in any capacity:
        //
        //    addList(oldRecord.unlinkedBibIds, oldRecord.bibIds) = {1, 2, 3, 4, 5}
        //
        // Any bibIds in this list which are _not_ on the new record should be unlinked.
        //
        //    unlinkedBibIds
        //      = addList(oldRecord.unlinkedBibIds, oldRecord.bibIds) - newRecord.bibIds
        //      = (all bibIds on old record) - (current bibIds on new record)
        //      = {1, 2, 3, 4, 5} - {3, 4}
        //      = {1, 2, 5}
        //
        unlinkedBibIds = subList(
          addList(existingRecord.unlinkedBibIds, existingRecord.bibIds),
          updatedRecord.bibIds)
      )
    } else {
      existingRecord
    }
  }

  private def addList(x: List[SierraRecordNumber], y: List[SierraRecordNumber]): List[SierraRecordNumber] = {
    (x.toSet ++ y.toSet).toList
  }

  private def subList(x: List[SierraRecordNumber], y: List[SierraRecordNumber]): List[SierraRecordNumber] = {
    (x.toSet -- y.toSet).toList
  }
}
