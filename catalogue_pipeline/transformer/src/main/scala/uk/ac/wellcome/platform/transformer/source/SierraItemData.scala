package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.platform.transformer.source.sierra.{
  Location => SierraLocation
}

case class SierraItemData(
  deleted: Boolean = false,
  location: Option[SierraLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
