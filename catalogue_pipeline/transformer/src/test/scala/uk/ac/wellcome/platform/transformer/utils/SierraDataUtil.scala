package uk.ac.wellcome.platform.transformer.utils

import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.platform.transformer.source.{SierraBibData, SierraMaterialType, VarField}
import uk.ac.wellcome.platform.transformer.source.sierra.{Language => SierraLanguage}

class SierraDataUtil extends IdentifiersUtil with SierraUtil {
  def createSierraBibDataWith(
    id: String = createSierraRecordNumberString,
    title: Option[String] = Some(randomAlphanumeric(25)),
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      id = id,
      title = title,
      lang = lang,
      materialType = materialType,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()
}
