package uk.ac.wellcome.platform.archive.archivist.bag
import java.io.InputStream

import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, ExternalIdentifier}

object BagInfoExtractor {
  def extractBagInfo[T](t: T, inputStream: InputStream): Either[ArchiveError[T],BagInfo] = {
    val bagInfoLines = scala.io.Source
      .fromInputStream(inputStream, "UTF-8")
      .mkString
      .split("\n")
    val regex = """(.*?)\s*:\s*(.*)\s*""".r

    bagInfoLines
      .collectFirst {
        case regex(key, value) if key == "External-Identifier" =>
          BagInfo(ExternalIdentifier(value))
      }
      .toRight(InvalidBagInfo(t))
  }
}
