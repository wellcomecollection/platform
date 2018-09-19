package uk.ac.wellcome.platform.archive.common.models

import uk.ac.wellcome.storage.ObjectLocation

case class BagItem(checksum: String, location: ObjectLocation)

object BagItem {

  def apply(
             fileChunk: String,
             bagName: BagPath,
             delimiter: String
           ): BagItem = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        BagItem(
          checksum,
          ObjectLocation(bagName.value, key)
        )
      case default =>
        throw MalformedBagDigestException(
          default.mkString(delimiter),
          bagName
        )
    }
  }
}

