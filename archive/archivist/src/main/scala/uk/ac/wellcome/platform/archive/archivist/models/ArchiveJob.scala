package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.{BagLocation, BagPath}

case class ArchiveJob(
                       zipFile: ZipFile,
                       bagLocation: BagLocation,
                       config: BagItConfig,
                       bagManifestLocations: List[BagManifestLocation]
                     ) {
  def digestDelimiter = config.digestDelimiterRegexp
}

object ArchiveJob {

  def create(
              zipFile: ZipFile,
              config: BagUploaderConfig
            ) = {

    val maybeBagPath = findBag(zipFile).toRight(new NoSuchElementException("bagit.txt"))

    maybeBagPath
      .map(bagPath =>
        (bagPath, BagManifestLocation.create(config.bagItConfig, bagPath)))
      .map { case (bagPath, bagManifestLocations) =>
      ArchiveJob(
        zipFile,
        BagLocation(
          storageNamespace = config.uploadConfig.uploadNamespace,
          storagePath = config.uploadConfig.uploadPrefix,
          bagPath = bagPath
        ),
        config.bagItConfig,
        bagManifestLocations
      )
    }
  }

  private def findBag(zipFile: ZipFile) = {
    val entries = zipFile.entries()
    val bagAnchor = """(.*?)\/*bagit.txt$""".r

    val bagPathString = Stream
      .continually(entries.nextElement)
      .map(_.getName)
      .collectFirst { case bagAnchor(path) => path }

    bagPathString.map(BagPath)
  }
}