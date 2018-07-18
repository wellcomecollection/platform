package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class SierraItemData(
  id: SierraRecordNumber,
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case object SierraItemData {
  def apply(
    id: String,
    deleted: Boolean = false,
    location: Option[SierraItemLocation] = None,
    fixedFields: Map[String, FixedField] = Map(),
    varFields: List[VarField] = List()
  ): SierraItemData =
    SierraItemData(
      id = SierraRecordNumber(id),
      deleted = deleted,
      location = location,
      fixedFields = fixedFields,
      varFields = varFields
    )
}

case class SierraItemLocation(code: String, name: String)
