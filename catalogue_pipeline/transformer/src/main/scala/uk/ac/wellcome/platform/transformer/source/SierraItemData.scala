package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class SierraItemData(
  id: SierraRecordNumber,
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case class SierraItemLocation(code: String, name: String)
