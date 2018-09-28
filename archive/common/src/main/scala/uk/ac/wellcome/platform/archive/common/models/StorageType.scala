package uk.ac.wellcome.platform.archive.common.models

sealed trait StorageType
case object DigitisedStorageType extends StorageType {
  override def toString = "digitised"
}