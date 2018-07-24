package uk.ac.wellcome.platform.transformer.utils

import io.circe.Encoder
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.platform.transformer.source._

import uk.ac.wellcome.platform.transformer.source.sierra.{
  Language => SierraLanguage
}
import uk.ac.wellcome.utils.JsonUtil._

trait SierraDataUtil extends IdentifiersUtil with SierraUtil {
  def createSierraBibDataWith(
    title: Option[String] = Some(randomAlphanumeric(25)),
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      id = createSierraRecordNumber,
      title = title,
      lang = lang,
      materialType = materialType,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()

  def createSierraItemDataWith(
    deleted: Boolean = false,
    location: Option[SierraItemLocation] = None
  ): SierraItemData =
    SierraItemData(
      id = createSierraRecordNumber,
      deleted = deleted,
      location = location
    )

  def createSierraItemData: SierraItemData = createSierraItemDataWith()

  // Based on https://circe.github.io/circe/codecs/custom-codecs.html
  //
  // This ensures that toJson(data) below stores the ID as {"id": "1234"}, and
  // not {"id": {"s": "1234"}}.
  //
  implicit val encodeSierraRecordNumber: Encoder[SierraRecordNumber] =
    Encoder.encodeString.contramap[SierraRecordNumber] { _.withoutCheckDigit }

  def createSierraItemRecordWith(data: SierraItemData): SierraItemRecord =
    createSierraItemRecordWith(
      id = data.id,
      data = toJson(data).get
    )

  def createSierraMaterialTypeWith(code: String = randomAlphanumeric(1),
                                   value: String = randomAlphanumeric(5)) = {
    SierraMaterialType(code, value)
  }

  def createSierraMaterialType: SierraMaterialType =
    createSierraMaterialTypeWith()

  def createSierraEbookMaterialType: SierraMaterialType =
    createSierraMaterialTypeWith(code = "v", value = "E-books")
}
