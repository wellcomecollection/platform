package uk.ac.wellcome.models

case class MergedSierraRecord(
  id: String,
  bibData: Option[String] = None,
  version: Int = 1
) {
  def mergeBibRecord(record: SierraRecord): Option[MergedSierraRecord] = {
    if(record.id != this.id) {
      throw new RuntimeException(s"Non-matching bib ids ${record.id} != ${this.id}")
    }


    Some(this.copy(
      bibData = Some(record.data),
      version = this.version + 1
    ))
  }
}

object MergedSierraRecord {
  def apply(id: String, bibData: String): MergedSierraRecord =
    MergedSierraRecord(id = id, bibData = Some(bibData))
}
