package uk.ac.wellcome.platform.archive.registrar.async.factories

import java.io.InputStream
import java.time.Instant

import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.common.bag.{BagDigestFileCreator, BagInfoParser}
import uk.ac.wellcome.platform.archive.common.models.bagit.{BagDigestFile, BagIt, BagLocation}
import uk.ac.wellcome.platform.archive.common.models.error.{ArchiveError, DownloadError}
import uk.ac.wellcome.platform.archive.common.progress.models.{InfrequentAccessStorageProvider, StorageLocation}
import uk.ac.wellcome.platform.archive.registrar.async.models.BagManifestUpdate
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

object StorageManifestFactory {
  def create(bagManifestUpdate: BagManifestUpdate)(implicit s3Client: AmazonS3)
    : Either[ArchiveError[BagManifestUpdate], StorageManifest] = {

    val algorithm = "sha256"

    for {
      bagInfoInputStream <- downloadFile(bagManifestUpdate, BagIt.BagInfoFilename)
      bagInfo <- BagInfoParser.parseBagInfo(
        bagManifestUpdate,
        bagInfoInputStream)
      manifestTuples <- getBagItems(
        bagManifestUpdate,
        s"manifest-$algorithm.txt",
        " +")
      tagManifestTuples <- getBagItems(
        bagManifestUpdate,
        s"tagmanifest-$algorithm.txt",
        " +")
    } yield {
      val checksumAlgorithm = ChecksumAlgorithm(algorithm)
      StorageManifest(
        space = bagManifestUpdate.archiveBagLocation.storageSpace,
        info = bagInfo,
        manifest = FileManifest(checksumAlgorithm, manifestTuples),
        tagManifest = FileManifest(checksumAlgorithm, tagManifestTuples),
        accessLocation = StorageLocation(
          provider = InfrequentAccessStorageProvider,
          location = bagManifestUpdate.accessBagLocation.objectLocation
        ),
        archiveLocations = List(
          StorageLocation(
            provider = InfrequentAccessStorageProvider,
            location = bagManifestUpdate.archiveBagLocation.objectLocation)),
        createdDate = Instant.now()
      )
    }
  }

  private def getBagItems(bagManifestUpdate: BagManifestUpdate,
                          name: String,
                          delimiter: String)(implicit s3Client: AmazonS3)
    : Either[ArchiveError[BagManifestUpdate], List[BagDigestFile]] = {
    val triedLines = downloadFile(bagManifestUpdate, name)
      .map(
        inputStream =>
          scala.io.Source
            .fromInputStream(inputStream)
            .mkString
            .split("\n")
            .filter(_.nonEmpty)
            .toList)

    triedLines.flatMap { lines: List[String] =>
      lines.traverse { line =>
        BagDigestFileCreator.create(line, bagManifestUpdate, name)
      }
    }
  }

  import uk.ac.wellcome.platform.archive.common.ConvertibleToInputStream._

  private def downloadFile(bagManifestUpdate: BagManifestUpdate,
                           filename: String)(implicit s3Client: AmazonS3)
    : Either[DownloadError[BagManifestUpdate], InputStream] = {
    val location: ObjectLocation =
      getFileObjectLocation(bagManifestUpdate.archiveBagLocation, filename)
    location.toInputStream.toEither
      .leftMap(ex => DownloadError(ex, location, bagManifestUpdate))
  }

  private def getFileObjectLocation(bagLocation: BagLocation, name: String) =
    ObjectLocation(
      namespace = bagLocation.storageNamespace,
      key = List(bagLocation.completePath, name).mkString("/")
    )
}
