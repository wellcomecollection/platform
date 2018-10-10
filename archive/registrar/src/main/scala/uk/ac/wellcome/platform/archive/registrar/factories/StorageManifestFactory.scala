package uk.ac.wellcome.platform.archive.registrar.factories
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.error.{ArchiveError, DownloadError, InvalidBagManifestError}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation}
import uk.ac.wellcome.platform.archive.registrar.models
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object StorageManifestFactory extends Logging {
  def create(archiveComplete: ArchiveComplete)(
    implicit s3Client: AmazonS3): Either[ArchiveError[ArchiveComplete], StorageManifest] = {
    val bagLocation = archiveComplete.bagLocation

    val algorithm = "sha256"

    val manifestTupleFuture =
      getBagItems(archiveComplete, s"manifest-$algorithm.txt", " +")
//    val tagManifestTupleFuture = getTuples(s"tagmanifest-$algorithm.txt", " +")

    val sourceIdentifier = SourceIdentifier(
      IdentifierType("source", "Label"),
      value = "123"
    )

    val location = DigitalLocation(
      List(
        s"http://${bagLocation.storageNamespace}.s3.amazonaws.com",
        bagLocation.storagePath,
        bagLocation.bagPath
      ).mkString("/"),
      LocationType(
        "aws-s3-standard-ia",
        "AWS S3 Standard IA"
      )
    )

    for {
      manifestTuples <- manifestTupleFuture
      fileManifest = FileManifest(
        ChecksumAlgorithm(algorithm),
        manifestTuples
      )
      //tagManifestTuples <- tagManifestTupleFuture
      tagManifest = TagManifest(
        ChecksumAlgorithm(algorithm),
        Nil //createBagDigestFiles(tagManifestTuples).toList
      )
    } yield
      StorageManifest(
        id = BagId(bagLocation.bagPath.value),
        source = sourceIdentifier,
        identifiers = List(sourceIdentifier),
        manifest = fileManifest,
        tagManifest = tagManifest,
        locations = List(location)
      )
  }

  def getBagItems(archiveComplete: ArchiveComplete, name: String, delimiter: String)(
    implicit s3Client: AmazonS3)
    : Either[ArchiveError[ArchiveComplete], List[BagDigestFile]] = {
    val location = getFileObjectLocation(archiveComplete.bagLocation, name)
    val triedLines = Try(s3Client.getObject(location.namespace, location.key))
      .map(_.getObjectContent)
      .toEither
      .leftMap(ex => DownloadError(ex, location ,archiveComplete))
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

  def getFileObjectLocation(bagLocation: BagLocation, name: String) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        name
      ).mkString("/")
    )

  def parseManifestLine(
    fileChunk: String,
    delimiter: String,
    archiveComplete: ArchiveComplete,
    manifestName: String): Either[InvalidBagManifestError[ArchiveComplete], BagDigestFile] = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        Right(
          models.BagDigestFile(
            Checksum(checksum),
            BagFilePath(key)
          ))
      case _ =>
        Left(InvalidBagManifestError(archiveComplete, manifestName))
    }
  }
}
