package uk.ac.wellcome.platform.archive.common.models

import uk.ac.wellcome.storage.ObjectLocation

case class BagLocation(storageNamespace: String,
                       storageRootPath: String,
                       bagPath: BagPath) {
  def bagPathInStorage = s"$storageRootPath/$bagPath"

  def getFileObjectLocation(filename: String): ObjectLocation =
    ObjectLocation(
      storageNamespace,
      List(storageRootPath, bagPath.value, filename).mkString("/")
    )
}


