package uk.ac.wellcome.platform.transformer.sierra.source

case class SierraItemData(
  deleted: Boolean = false,
  location: Option[SierraLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
