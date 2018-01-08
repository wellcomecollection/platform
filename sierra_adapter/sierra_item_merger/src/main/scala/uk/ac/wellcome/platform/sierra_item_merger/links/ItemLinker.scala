package uk.ac.wellcome.platform.sierra_item_merger.links

import uk.ac.wellcome.models.transformable.MergedSierraRecord
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

object ItemLinker {

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns the merged record.
    */
  def linkItemRecord(mergedSierraRecord: MergedSierraRecord,
                     itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.bibIds.contains(mergedSierraRecord.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${mergedSierraRecord.id} in item bib ${itemRecord.bibIds}")
    }

    // We can decide whether to insert the new data in two steps:
    //
    //  - Do we already have any data for this item?  If not, we definitely
    //    need to merge this record.
    //  - If we have existing data, is it newer or older than the update we've
    //    just received?  If the existing data is older, we need to merge the
    //    new record.
    //
    val isNewerData = mergedSierraRecord.itemData.get(itemRecord.id) match {
      case Some(existing) =>
        itemRecord.modifiedDate.isAfter(existing.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      val itemData = mergedSierraRecord.itemData + (itemRecord.id -> itemRecord)
      mergedSierraRecord.copy(itemData = itemData)
    } else {
      mergedSierraRecord
    }
  }
}
