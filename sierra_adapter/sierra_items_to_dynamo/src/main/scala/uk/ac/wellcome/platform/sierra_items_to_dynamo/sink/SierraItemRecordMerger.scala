package uk.ac.wellcome.platform.sierra_items_to_dynamo.sink

import uk.ac.wellcome.models.SierraItemRecord

object SierraItemRecordMerger {
  def mergeItems(oldRecord: SierraItemRecord, newRecord: SierraItemRecord): SierraItemRecord = {
    newRecord.copy(
      unlinkedBibIds = oldRecord.unlinkedBibIds ++ (oldRecord.bibIds.toSet -- newRecord.bibIds.toSet).toList
    )
  }
}
