package uk.ac.wellcome.platform.transformer.sierra.source

import uk.ac.wellcome.platform.transformer.sierra.source.sierra.Country
import uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.{SierraLanguage, SierraLocation}

// https://techdocs.iii.com/sierraapi/Content/zReference/objects/bibObjectDescription.htm
case class SierraBibData(
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[Country] = None,
  lang: Option[SierraLanguage] = None,
  materialType: Option[SierraMaterialType] = None,
  locations: Option[List[SierraLocation]] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
