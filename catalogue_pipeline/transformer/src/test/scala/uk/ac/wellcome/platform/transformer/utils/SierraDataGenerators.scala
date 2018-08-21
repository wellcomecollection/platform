package uk.ac.wellcome.platform.transformer.utils

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.models.work.test.util.IdentifiersGenerators
import uk.ac.wellcome.platform.transformer.source._
import uk.ac.wellcome.platform.transformer.source.sierra.{
  Language => SierraLanguage,
  Location => SierraLocation
}
import uk.ac.wellcome.json.JsonUtil._

trait SierraDataGenerators extends IdentifiersGenerators with SierraGenerators {
  def createSierraBibDataWith(
    title: Option[String] = Some(randomAlphanumeric(25)),
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    locations: Option[List[SierraLocation]] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      title = title,
      lang = lang,
      materialType = materialType,
      locations = locations,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()

  def bibData(marcTag: String, marcSubfields: List[MarcSubfield]) = {
    createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = marcTag,
          indicator1 = "",
          indicator2 = "",
          subfields = marcSubfields
        )
      )
    )
  }

  def createSierraItemDataWith(
    deleted: Boolean = false,
    location: Option[SierraLocation] = None
  ): SierraItemData =
    SierraItemData(
      deleted = deleted,
      location = location
    )

  def createSierraItemData: SierraItemData = createSierraItemDataWith()

  def createSierraItemRecordWith(data: SierraItemData): SierraItemRecord =
    createSierraItemRecordWith(
      data = toJson(data).get
    )

  def createSierraMaterialTypeWith(code: String): SierraMaterialType =
    SierraMaterialType(code = code)
}
