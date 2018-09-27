package uk.ac.wellcome.platform.archive.archivist.bag
import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath, EntryPath}

import scala.util.Try

object ArchiveJobCreator {
  def create(
              zipFile: ZipFile,
              config: BagUploaderConfig
            ) = {

    getBagIdentifier(zipFile).map(bagIdentifier => BagPath(s"$DigitisedStorageType/$bagIdentifier"))
      .map { bagPath =>
        ArchiveJob(
          zipFile,
          BagLocation(
            storageNamespace = config.uploadConfig.uploadNamespace,
            storagePath = config.uploadConfig.uploadPrefix,
            bagPath = bagPath
          ),
          config.bagItConfig,
          BagManifestLocation.create(config.bagItConfig)
        )
      }.toEither
  }

  private def getBagIdentifier(zipFile: ZipFile): Try[String] = {
    Try(ZipFileReader.maybeInputStream(ZipLocation(zipFile, EntryPath("bag-info.txt"))).getOrElse(throw new NoSuchElementException("Unable to read from bag-info.txt"))).flatMap{ inputStream =>
      val bagInfoLines = scala.io.Source.fromInputStream(inputStream, "UTF-8").mkString.split("\n")
      val regex = """(.*?)\s*:\s*(.*)\s*""".r
      Try(bagInfoLines.collectFirst { case regex(key,value) if key == "External-Identifier" => value}.getOrElse(throw new NoSuchElementException("Unable to extract External-Indentifier from bag-info.txt")))
    }
  }
}
