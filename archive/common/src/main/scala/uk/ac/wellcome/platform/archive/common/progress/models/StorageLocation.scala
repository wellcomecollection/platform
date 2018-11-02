package uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.platform.archive.common.models.{
  DisplayLocation,
  DisplayProvider
}
import uk.ac.wellcome.storage.ObjectLocation

case class StorageLocation(provider: StorageProvider, location: ObjectLocation)
object StorageLocation {
  def apply(displayLocation: DisplayLocation): StorageLocation =
    StorageLocation(
      StorageProvider(displayLocation.provider),
      ObjectLocation(displayLocation.bucket, displayLocation.path))
}

case class StorageProvider(id: String)
object StorageProvider {
  def apply(displayProvider: DisplayProvider): StorageProvider =
    StorageProvider(displayProvider.id)
}
