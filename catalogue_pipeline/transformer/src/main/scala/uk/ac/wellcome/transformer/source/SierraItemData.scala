package uk.ac.wellcome.transformer.source

case class SierraItemData(
  id: String,
  deleted: Boolean = false,
  location: Option[SierraLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case class SierraLocation(code: String, name: String)
