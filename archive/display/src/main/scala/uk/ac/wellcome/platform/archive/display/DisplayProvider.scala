package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.platform.archive.common.progress.models.StorageProvider

import scala.annotation.meta.field

@ApiModel(value = "Provider")
case class DisplayProvider(
  id: String,
  @JsonKey("type")
  @(ApiModelProperty @field)(name = "type", allowableValues = "Provider")
  ontologyType: String = "Provider") {
  def toStorageProvider: StorageProvider = StorageProvider(id)
}
object DisplayProvider {
  def apply(provider: StorageProvider): DisplayProvider =
    DisplayProvider(id = provider.id)
}
