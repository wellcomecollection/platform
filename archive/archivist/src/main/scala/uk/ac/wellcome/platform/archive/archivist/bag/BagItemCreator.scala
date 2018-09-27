package uk.ac.wellcome.platform.archive.archivist.bag
import uk.ac.wellcome.platform.archive.common.models.{BagItem, BagPath, EntryPath, MalformedBagDigestException}

import scala.util.Try

object BagItemCreator {
  def create(
    fileChunk: String,
    bagName: BagPath,
    delimiter: String
  ): Try[BagItem] = Try {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        BagItem(
          checksum,
          EntryPath(key)
        )
      case default =>
        throw MalformedBagDigestException(
          default.mkString(delimiter),
          bagName
        )
    }
  }
}
