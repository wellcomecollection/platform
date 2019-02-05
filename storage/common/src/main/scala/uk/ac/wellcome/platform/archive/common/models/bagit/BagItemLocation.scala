package uk.ac.wellcome.platform.archive.common.models.bagit

import uk.ac.wellcome.storage.ObjectLocation

case class BagItemLocation(
  bagLocation: BagLocation,
  bagItemPath: BagItemPath
) {
  def completePath: String =
    List(bagLocation.completePath, bagItemPath).mkString("/")

  def objectLocation: ObjectLocation =
    ObjectLocation(
      namespace = bagLocation.storageNamespace,
      key = completePath
    )
}
