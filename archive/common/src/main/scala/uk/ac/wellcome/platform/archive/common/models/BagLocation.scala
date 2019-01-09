package uk.ac.wellcome.platform.archive.common.models

case class BagLocation(storageNamespace: String,
                       storageRootPath: String,
                       bagPath: BagPath) {
  def bagPathInStorage = s"$storageRootPath/$bagPath"
}
