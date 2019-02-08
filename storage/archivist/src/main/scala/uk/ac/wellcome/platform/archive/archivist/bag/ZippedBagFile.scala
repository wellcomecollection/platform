package uk.ac.wellcome.platform.archive.archivist.bag

import java.io.FileNotFoundException
import java.util.zip.ZipFile

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.bagit.BagIt

import scala.collection.JavaConverters._
import scala.util.Try

object ZippedBagFile extends Logging {
  private val endsWithBagInfoFilenameRegexp = (BagIt.BagInfoFilename + "$").r

  def bagPathFromBagInfoPath(bagInfo: String): Option[String] = {
    endsWithBagInfoFilenameRegexp replaceFirstIn (bagInfo, "") match {
      case ""        => None
      case s: String => Some(s)
    }
  }

  def locateBagInfo(zipFile: ZipFile): Try[String] = Try {
    zipFile
      .entries()
      .asScala
      .filter { e =>
        e.getName.split("/").last == BagIt.BagInfoFilename && !e.isDirectory
      }
      .map(_.getName)
      .toList match {
      case Seq(bagInfo) =>
        bagInfo
      case Seq() =>
        throw new FileNotFoundException(
          s"'${BagIt.BagInfoFilename}' not found.")
      case matchingBagInfoFiles: Seq[_] =>
        throw new IllegalArgumentException(
          s"Expected only one '${BagIt.BagInfoFilename}' found $matchingBagInfoFiles.")
    }
  }
}
