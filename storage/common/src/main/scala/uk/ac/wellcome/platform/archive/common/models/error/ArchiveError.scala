package uk.ac.wellcome.platform.archive.common.models.error
import uk.ac.wellcome.storage.ObjectLocation

trait ArchiveError[T] {
  val t: T
}

case class DownloadError[T](exception: Throwable,
                            location: ObjectLocation,
                            t: T)
    extends ArchiveError[T] {
  override def toString =
    s"There was an exception while downloading object $location: ${exception.getMessage}"
}

case class InvalidBagManifestError[T](t: T, manifestName: String, line: String)
    extends ArchiveError[T] {
  override def toString = s"Invalid bag manifest $manifestName: $line"
}

case class InvalidBagInfo[T](t: T, keys: List[String]) extends ArchiveError[T] {
  override def toString = s"Invalid bag-info.txt: ${keys.mkString(", ")}"
}
