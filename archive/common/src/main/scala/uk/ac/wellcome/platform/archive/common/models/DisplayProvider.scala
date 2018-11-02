package uk.ac.wellcome.platform.archive.common.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.StorageProvider

case class DisplayProvider(id: String,
                           @JsonKey("type") ontologyType: String = "Provider")
object DisplayProvider {
  def apply(provider: StorageProvider): DisplayProvider =
    DisplayProvider(id = provider.id)
}
