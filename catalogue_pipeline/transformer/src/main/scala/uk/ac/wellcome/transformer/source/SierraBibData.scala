package uk.ac.wellcome.transformer.source

case class SierraBibData(
  id: String,
  title: String,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
