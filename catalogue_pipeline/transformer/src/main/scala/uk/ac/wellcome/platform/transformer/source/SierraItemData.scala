package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class SierraItemData(
  id: String,
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
) {
  val sierraId = SierraRecordNumber(id)
}

case class SierraItemLocation(code: String, name: String)
