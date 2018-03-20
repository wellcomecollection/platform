package uk.ac.wellcome.transformer.source

import uk.ac.wellcome.transformer.source.sierra.{Country, Language}

case class SierraBibData(
  id: String,
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[Country] = None,
  lang: Option[Language] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
