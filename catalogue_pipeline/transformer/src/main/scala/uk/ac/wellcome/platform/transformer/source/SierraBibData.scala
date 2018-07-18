package uk.ac.wellcome.platform.transformer.source

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.platform.transformer.source.sierra.{Country => SierraCountry, Language => SierraLanguage}

case class SierraBibData(
  id: SierraRecordNumber,
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[SierraCountry] = None,
  lang: Option[SierraLanguage] = None,
  materialType: Option[SierraMaterialType] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case object SierraBibData {
  def apply(
    id: String,
    title: Option[String] = None,
    deleted: Boolean = false,
    suppressed: Boolean = false,
    country: Option[SierraCountry] = None,
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    fixedFields: Map[String, FixedField] = Map(),
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      id = SierraRecordNumber(id),
      title = title,
      deleted = deleted,
      suppressed = false,
      country = country,
      lang = lang,
      materialType = materialType,
      fixedFields = fixedFields,
      varFields = varFields
    )
}
