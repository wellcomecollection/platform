package uk.ac.wellcome.platform.sierra_bib_merger.merger

import uk.ac.wellcome.models.{MergedSierraRecord, SierraBibRecord}

object BibMerger {

  /** Return the most up-to-date combination of the merged record and the
    * bib record we've just received.
    */
  def mergeBibRecord(mergedSierraRecord: MergedSierraRecord,
                     sierraBibRecord: SierraBibRecord): MergedSierraRecord = {
    if (sierraBibRecord.id != mergedSierraRecord.id) {
      throw new RuntimeException(
        s"Non-matching bib ids ${sierraBibRecord.id} != ${mergedSierraRecord.id}")
    }

    val isNewerData = mergedSierraRecord.maybeBibData match {
      case Some(bibData) =>
        sierraBibRecord.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      mergedSierraRecord.copy(maybeBibData = Some(sierraBibRecord))
    } else {
      mergedSierraRecord
    }
  }

}
