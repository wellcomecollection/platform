package uk.ac.wellcome.platform.transformer.sierra.source

import uk.ac.wellcome.platform.transformer.sierra.source.sierra.{
  SierraSourceCountry,
  SierraSourceLanguage,
  SierraSourceLocation
}

// https://techdocs.iii.com/sierraapi/Content/zReference/objects/bibObjectDescription.htm
case class SierraBibData(
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[SierraSourceCountry] = None,
  lang: Option[SierraSourceLanguage] = None,
  materialType: Option[SierraMaterialType] = None,
  locations: Option[List[SierraSourceLocation]] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
