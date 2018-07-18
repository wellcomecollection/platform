package uk.ac.wellcome.models.transformable.sierra

/** Represents the seven-digit IDs that are used to identify resources
  * in Sierra.
  */
case class SierraRecordNumber(s: String) {
  require(
    """^[0-9]{7}$""".r.unapplySeq(s) isDefined,
    s"Not a 7-digit Sierra record number: $s"
  )
}
