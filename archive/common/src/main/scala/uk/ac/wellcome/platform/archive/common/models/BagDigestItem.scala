package uk.ac.wellcome.platform.archive.common.models

import uk.ac.wellcome.storage.ObjectLocation

case class BagContentItem(checksum: Option[String], location: ObjectLocation)
object BagContentItem {
  def apply(checksum: String, location: ObjectLocation): BagContentItem = {
    BagContentItem(Some(checksum), location)
  }
  def apply(location: ObjectLocation): BagContentItem = {
    BagContentItem(None, location)
  }
}
