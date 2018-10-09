package uk.ac.wellcome.platform.archive.registrar.models.errors
import uk.ac.wellcome.platform.archive.common.models.BagLocation

sealed trait RegistrarError

case class FileNotFoundError(bagLocation: BagLocation) extends RegistrarError

case class InvalidBagManifestError(bagLocation: BagLocation, manifestName: String) extends RegistrarError