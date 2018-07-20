package uk.ac.wellcome.platform.transformer.source

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class SierraItemData(
  @JsonKey("id") _id: String,
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
) {
  val id = SierraRecordNumber(_id)
}

case class SierraItemLocation(code: String, name: String)
