package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.platform.transformer.source.sierra.{
  Country => SierraCountry,
  Language => SierraLanguage
}

case class SierraBibData(
  id: String,
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[SierraCountry] = None,
  lang: Option[SierraLanguage] = None,
  materialType: Option[SierraMaterialType] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
