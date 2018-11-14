package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.platform.archive.common.progress.models.StorageLocation
import uk.ac.wellcome.storage.ObjectLocation

import scala.annotation.meta.field

@ApiModel(value = "Location")
case class DisplayLocation(
  provider: DisplayProvider,
  bucket: String,
  path: String,
  @JsonKey("type")
  @(ApiModelProperty @field)(name = "type", allowableValues = "Location")
  ontologyType: String = "Location") {
  def toStorageLocation: StorageLocation =
    StorageLocation(provider.toStorageProvider, ObjectLocation(bucket, path))
}
object DisplayLocation {
  def apply(location: StorageLocation): DisplayLocation =
    DisplayLocation(
      DisplayProvider(location.provider),
      location.location.namespace,
      location.location.key)
}
