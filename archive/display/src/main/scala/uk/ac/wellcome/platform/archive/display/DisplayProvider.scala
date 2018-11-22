package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.StorageProvider

case class DisplayProvider(id: String,
                           @JsonKey("type") ontologyType: String = "Provider") {
  def toStorageProvider: StorageProvider = StorageProvider(id)
}
object DisplayProvider {
  def apply(provider: StorageProvider): DisplayProvider =
    DisplayProvider(id = provider.id)
}
