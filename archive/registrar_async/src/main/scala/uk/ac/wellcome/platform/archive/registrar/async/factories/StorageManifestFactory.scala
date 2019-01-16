package uk.ac.wellcome.platform.archive.registrar.async.factories

import java.io.InputStream
import java.time.Instant

import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.common.bag.{
  BagDigestFileCreator,
  BagInfoParser
}
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  DownloadError
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagDigestFile,
  BagLocation
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  InfrequentAccessStorageProvider,
  StorageLocation
}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

object StorageManifestFactory {
  def create(archiveComplete: ArchiveComplete)(implicit s3Client: AmazonS3)
    : Either[ArchiveError[ArchiveComplete], StorageManifest] = {

    val algorithm = "sha256"

    for {
      bagInfoInputStream <- downloadFile(archiveComplete, "bag-info.txt")
      bagInfo <- BagInfoParser.parseBagInfo(archiveComplete, bagInfoInputStream)
      manifestTuples <- getBagItems(
        archiveComplete,
        s"manifest-$algorithm.txt",
        " +")
      tagManifestTuples <- getBagItems(
        archiveComplete,
        s"tagmanifest-$algorithm.txt",
        " +")
    } yield {
      val checksumAlgorithm = ChecksumAlgorithm(algorithm)
      StorageManifest(
        space = archiveComplete.space,
        info = bagInfo,
        manifest = FileManifest(checksumAlgorithm, manifestTuples),
        tagManifest = FileManifest(checksumAlgorithm, tagManifestTuples),
        accessLocation = StorageLocation(
          InfrequentAccessStorageProvider,
          ObjectLocation(
            archiveComplete.bagLocation.storageNamespace,
            s"${archiveComplete.bagLocation.storageRootPath}/${archiveComplete.bagLocation.bagPath.value}")
        ),
        createdDate = Instant.now()
      )
    }
  }

  private def getBagItems(archiveComplete: ArchiveComplete,
                          name: String,
                          delimiter: String)(implicit s3Client: AmazonS3)
    : Either[ArchiveError[ArchiveComplete], List[BagDigestFile]] = {
    val triedLines = downloadFile(archiveComplete, name)
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
        BagDigestFileCreator.create(line, archiveComplete, name)
      }
    }
  }

  import uk.ac.wellcome.platform.archive.common.ConvertibleToInputStream._

  private def downloadFile(archiveComplete: ArchiveComplete, filename: String)(
    implicit s3Client: AmazonS3)
    : Either[DownloadError[ArchiveComplete], InputStream] = {
    val location: ObjectLocation =
      getFileObjectLocation(archiveComplete.bagLocation, filename)
    location.toInputStream.toEither
      .leftMap(ex => DownloadError(ex, location, archiveComplete))
  }

  private def getFileObjectLocation(bagLocation: BagLocation, name: String) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storageRootPath,
        bagLocation.bagPath.value,
        name
      ).mkString("/")
    )
}
