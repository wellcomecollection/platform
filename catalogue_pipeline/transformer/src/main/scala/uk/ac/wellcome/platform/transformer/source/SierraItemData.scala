package uk.ac.wellcome.platform.transformer.source

case class SierraItemData(
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case class SierraItemLocation(code: String, name: String)
