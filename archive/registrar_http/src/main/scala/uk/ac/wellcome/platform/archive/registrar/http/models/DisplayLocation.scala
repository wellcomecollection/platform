package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.registrar.common.models.Location

case class DisplayLocation(provider: DisplayProvider, bucket: String, path: String, @JsonKey("type") ontologyType: String = "Location")
object DisplayLocation {
  def apply(location: Location): DisplayLocation = DisplayLocation(DisplayProvider(location.provider), location.location.namespace, location.location.key)
}