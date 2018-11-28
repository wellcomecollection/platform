package uk.ac.wellcome.platform.sierra_item_merger.links

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

object ItemUnlinker {

  def unlinkItemRecord(sierraTransformable: SierraTransformable,
                       itemRecord: SierraItemRecord): SierraTransformable = {
    if (!itemRecord.unlinkedBibIds.contains(sierraTransformable.sierraId)) {
      throw new RuntimeException(
        s"Non-matching bib id ${sierraTransformable.sierraId} in item unlink bibs ${itemRecord.unlinkedBibIds}")
    }

    val itemRecords =
      sierraTransformable.itemRecords
        .filterNot {
          case (id, currentItemRecord) =>
            val matchesCurrentItemRecord = id == itemRecord.id

            val modifiedAfter = itemRecord.modifiedDate.isAfter(
              currentItemRecord.modifiedDate
            )

            matchesCurrentItemRecord && modifiedAfter
        }

    sierraTransformable.copy(itemRecords = itemRecords)
  }
}
