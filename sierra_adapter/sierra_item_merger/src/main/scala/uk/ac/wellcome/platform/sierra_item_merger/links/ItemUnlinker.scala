package uk.ac.wellcome.platform.sierra_item_merger.links

import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}

object ItemUnlinker {

  def unlinkItemRecord(mergedSierraRecord: MergedSierraRecord,itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.unlinkedBibIds.contains(mergedSierraRecord.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${mergedSierraRecord.id} in item unlink bibs ${itemRecord.unlinkedBibIds}")
    }

    val itemData: Map[String, SierraItemRecord] = mergedSierraRecord.itemData
      .filterNot {
        case (id, currentItemRecord) => {
          val matchesCurrentItemRecord = id == itemRecord.id

          val modifiedAfter = itemRecord.modifiedDate.isAfter(
            currentItemRecord.modifiedDate
          )

          matchesCurrentItemRecord && modifiedAfter
        }
      }

    mergedSierraRecord.copy(itemData = itemData)
  }
}
