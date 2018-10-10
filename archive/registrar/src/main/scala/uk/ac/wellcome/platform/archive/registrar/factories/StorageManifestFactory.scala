package uk.ac.wellcome.platform.archive.registrar.factories
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.BagLocation
import uk.ac.wellcome.platform.archive.registrar.models
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.platform.archive.registrar.models.errors.{
  FileNotFoundError,
  InvalidBagManifestError,
  RegistrarError
}
import uk.ac.wellcome.storage.ObjectLocation

import scala.util.Try

object StorageManifestFactory extends Logging {
  def create(bagLocation: BagLocation)(
    implicit s3Client: AmazonS3): Either[RegistrarError, StorageManifest] = {

    val algorithm = "sha256"

    val manifestTupleFuture =
      getBagItems(bagLocation, s"manifest-$algorithm.txt", " +")
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

  def getBagItems(bagLocation: BagLocation, name: String, delimiter: String)(
    implicit s3Client: AmazonS3)
    : Either[RegistrarError, List[BagDigestFile]] = {
    val location = createBagItMetaFileLocation(bagLocation, name)
    val triedLines = Try(s3Client.getObject(location.namespace, location.key))
      .map(_.getObjectContent)
      .toEither
      .leftMap(_ => FileNotFoundError(bagLocation))
      .map(
        inputStream =>
          scala.io.Source
            .fromInputStream(inputStream)
            .mkString
            .split("\n")
            .filter(_.nonEmpty)
            .toList)

    triedLines.flatMap { lines: List[String] =>
      val value: Either[InvalidBagManifestError, List[BagDigestFile]] =
        lines.map { line =>
          parseManifestLine(line, delimiter, bagLocation, name)
        }.sequence
      value
    }
  }

  def createBagItMetaFileLocation(bagLocation: BagLocation, name: String) =
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
    job: BagLocation,
    manifestName: String): Either[InvalidBagManifestError, BagDigestFile] = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        Right(
          models.BagDigestFile(
            Checksum(checksum),
            BagFilePath(key)
          ))
      case _ =>
        Left(InvalidBagManifestError(job, manifestName))
    }
  }
}
