package uk.ac.wellcome.platform.sierra_item_merger.links

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

object ItemLinker {

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns the merged record.
    */
  def linkItemRecord(sierraTransformable: SierraTransformable,
                     itemRecord: SierraItemRecord): SierraTransformable = {
    if (!itemRecord.bibIds.contains(sierraTransformable.sourceId)) {
      throw new RuntimeException(
        s"Non-matching bib id ${sierraTransformable.sourceId} in item bib ${itemRecord.bibIds}")
    }

    // We can decide whether to insert the new data in two steps:
    //
    //  - Do we already have any data for this item?  If not, we definitely
    //    need to merge this record.
    //  - If we have existing data, is it newer or older than the update we've
    //    just received?  If the existing data is older, we need to merge the
    //    new record.
    //
    val isNewerData = sierraTransformable.itemData.get(itemRecord.id) match {
      case Some(existing) =>
        itemRecord.modifiedDate.isAfter(existing.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      val itemData = sierraTransformable.itemData + (itemRecord.id -> itemRecord)
      sierraTransformable.copy(itemData = itemData)
    } else {
      sierraTransformable
    }
  }
}
