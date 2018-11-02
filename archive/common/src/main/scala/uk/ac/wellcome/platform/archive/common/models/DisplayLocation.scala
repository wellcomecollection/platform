package uk.ac.wellcome.platform.archive.common.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.StorageLocation

case class DisplayLocation(provider: DisplayProvider,
                           bucket: String,
                           path: String,
                           @JsonKey("type") ontologyType: String = "Location")
object DisplayLocation {
  def apply(location: StorageLocation): DisplayLocation =
    DisplayLocation(
      DisplayProvider(location.provider),
      location.location.namespace,
      location.location.key)
}
