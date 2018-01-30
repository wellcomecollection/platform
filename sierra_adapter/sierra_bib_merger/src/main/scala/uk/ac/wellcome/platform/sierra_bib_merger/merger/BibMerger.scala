package uk.ac.wellcome.platform.sierra_bib_merger.merger

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

object BibMerger {

  /** Return the most up-to-date combination of the merged record and the
    * bib record we've just received.
    */
  def mergeBibRecord(sierraTransformable: SierraTransformable,
                     sierraBibRecord: SierraBibRecord): SierraTransformable = {
    if (sierraBibRecord.id != sierraTransformable.sourceId) {
      throw new RuntimeException(
        s"Non-matching bib ids ${sierraBibRecord.id} != ${sierraTransformable.sourceId}")
    }

    val isNewerData = sierraTransformable.maybeBibData match {
      case Some(bibData) =>
        sierraBibRecord.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      sierraTransformable.copy(maybeBibData = Some(sierraBibRecord))
    } else {
      sierraTransformable
    }
  }

}
