package uk.ac.wellcome.platform.archive.registrar.async.factories

import java.io.InputStream
import java.time.Instant

import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.bag.BagInfoParser
import uk.ac.wellcome.platform.archive.common.models.error.{ArchiveError, DownloadError, InvalidBagManifestError}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object StorageManifestFactory extends Logging {
  def create(archiveComplete: ArchiveComplete)(implicit s3Client: AmazonS3)
    : Either[ArchiveError[ArchiveComplete], StorageManifest] = {
    val algorithm = "sha256"

    for {
      bagInfoInputStream <- downloadFile(archiveComplete, "bag-info.txt")
      bagInfo <- BagInfoParser.parseBagInfo(archiveComplete, bagInfoInputStream)
      manifestTuples <- getBagItems(archiveComplete, s"manifest-$algorithm.txt", " +")
      fileManifest = FileManifest(
        ChecksumAlgorithm(algorithm),
        manifestTuples
      )
    } yield
      StorageManifest(
        space = archiveComplete.bagId.space,
        info = bagInfo,
        manifest = fileManifest,
        accessLocation = Location(
          Provider("aws-s3-ia", "AWS S3 - Infrequent Access"),
          ObjectLocation(
            archiveComplete.bagLocation.storageNamespace,
            s"${archiveComplete.bagLocation.storagePath}/${archiveComplete.bagLocation.bagPath.value}")
        ),
        createdDate = Instant.now()
      )
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
        parseManifestLine(line, delimiter, archiveComplete, name)
      }
    }
  }

  private def downloadFile(archiveComplete: ArchiveComplete, filename: String)(implicit s3Client: AmazonS3)
    : Either[DownloadError[ArchiveComplete], InputStream] = {
    val location = getFileObjectLocation(archiveComplete.bagLocation, filename)
    Try(s3Client.getObject(location.namespace, location.key))
        .map(_.getObjectContent)
        .toEither
        .leftMap(ex => DownloadError(ex, location, archiveComplete))
  }

  private def getFileObjectLocation(bagLocation: BagLocation, name: String) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        name
      ).mkString("/")
    )

  private def parseManifestLine(fileChunk: String,
                                delimiter: String,
                                archiveComplete: ArchiveComplete,
                                manifestName: String)
    : Either[InvalidBagManifestError[ArchiveComplete], BagDigestFile] = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        Right(
          BagDigestFile(
            Checksum(checksum),
            BagFilePath(key)
          ))
      case _ =>
        Left(InvalidBagManifestError(archiveComplete, manifestName))
    }
  }
}
