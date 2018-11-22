package uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.storage.ObjectLocation

case class StorageLocation(provider: StorageProvider, location: ObjectLocation)

case class StorageProvider(id: String)
