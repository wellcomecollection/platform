package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil

case class MergedSierraRecord(
  id: String,
  bibData: Option[SierraBibRecord] = None,
  version: Int = 1
) {
  def mergeBibRecord(record: SierraBibRecord): Option[MergedSierraRecord] = {
    if (record.id != this.id) {
      throw new RuntimeException(s"Non-matching bib ids ${record.id} != ${this.id}")
    }

    Some(this.copy(
      bibData = Some(record),
      version = this.version + 1
    ))
  }
}

object MergedSierraRecord {
  def apply(id: String, bibData: String): MergedSierraRecord = {
    val bibRecord = JsonUtil.fromJson[SierraBibRecord](bibData).get
    MergedSierraRecord(id = id, bibData = Some(bibRecord))
  }
}
