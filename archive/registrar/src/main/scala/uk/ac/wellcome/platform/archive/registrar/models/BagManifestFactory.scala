package uk.ac.wellcome.platform.archive.registrar.models

import akka.stream.Materializer
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.BagLocation

import scala.concurrent.ExecutionContext
import scala.util.Try

object BagManifestFactory extends Logging {
  def create(s3Client: AmazonS3, bagLocation: BagLocation)(
    implicit materializer: Materializer,
    executionContext: ExecutionContext
  ): Try[BagManifest] = Try {
    val algorithm = "sha256"

    val manifestFileName = BagMetaFileConfig(s"manifest-$algorithm.txt", " +")
    val filesWithChecksum =
      BagItMetaFileExtractor.get(s3Client, bagLocation, manifestFileName)

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

    val tagManifest = TagManifest(
      ChecksumAlgorithm("sha256"),
      Nil //createBagDigestFiles(tagManifestTuples).toList
    )

    val fileManifest = FileManifest(
      ChecksumAlgorithm(algorithm),
      filesWithChecksum.map {
        case (checksum: String, filePath: String) =>
          BagDigestFile.apply(checksum, filePath)
      }
    )

    BagManifest(
      id = BagId(bagLocation.bagPath.value),
      source = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      manifest = fileManifest,
      tagManifest = tagManifest,
      locations = List(location)
    )
  }
}
