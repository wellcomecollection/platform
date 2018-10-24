package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.registrar.common.models.Provider

case class DisplayProvider(id: String,
                           label: String,
                           @JsonKey("type") ontologyType: String = "Provider")
object DisplayProvider {
  def apply(provider: Provider): DisplayProvider =
    DisplayProvider(id = provider.id, label = provider.label)
}
